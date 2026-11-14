package br.joao.launcher.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.BatteryManager
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.graphics.PathParser
import br.joao.launcher.LauncherApp
import br.joao.launcher.R
import br.joao.launcher.data.BatteryItem
import br.joao.launcher.data.DeviceKind
import br.joao.launcher.data.WeatherState
import br.joao.launcher.ui.Glyphs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget para a tela de bloqueio (via Good Lock › LockStar) e também para a tela inicial:
 * baterias (celular sempre; relógio e fones só quando conectados) + pílula do clima.
 * É desenhado como uma imagem para ficar igual ao visual do launcher.
 */
class LockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as LauncherApp
                if (app.weather.isStale()) app.weather.refresh()
                ids.forEach { render(context, manager, it) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        render(context, manager, id)
    }

    companion object {
        private const val RED = 0xFFE0352C.toInt()
        private const val INK = 0xFFF4F6F8.toInt()
        private const val INK_MUTED = 0x9EF4F6F8.toInt()

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LockWidget::class.java))
            ids.forEach { runCatching { render(context, manager, it) } }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val app = context.applicationContext as LauncherApp
            val items = app.batteries.items.value.map { item ->
                if (item.kind == DeviceKind.PHONE && item.level < 0) phoneBattery(context) else item
            }
            val bmp = draw(context, items, app.weather.cached())
            val views = RemoteViews(context.packageName, R.layout.widget_lock)
            views.setImageViewBitmap(R.id.widget_image, bmp)
            manager.updateAppWidget(id, views)
        }

        private fun phoneBattery(context: Context): BatteryItem {
            val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            return BatteryItem(DeviceKind.PHONE, if (level >= 0 && scale > 0) level * 100 / scale else -1)
        }

        private fun typeface(context: Context, res: Int, weight: Int): Typeface = runCatching {
            val font = Font.Builder(context.resources, res).setFontVariationSettings("'wght' $weight").build()
            Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).build()
        }.getOrDefault(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))

        /**
         * Desenha no tamanho natural do conteúdo; o ImageView (fitCenter) amplia ou reduz
         * para ocupar a área inteira do widget sem cortar nada, com qualquer número de aparelhos.
         */
        private fun draw(context: Context, items: List<BatteryItem>, weather: WeatherState?): Bitmap {
            val d = context.resources.displayMetrics.density
            val unit = 24 * d
            val display = typeface(context, R.font.archivo, 800)
            val body = typeface(context, R.font.manrope, 600)

            // Medidas da linha das baterias.
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = display; textSize = unit * 0.95f }
            val icon = unit * 1.05f
            val gap = unit * 0.35f
            val spacing = unit * 1.1f
            val widths = items.map { b -> icon + gap + textPaint.measureText(label(b)) }
            val row1W = widths.sum() + spacing * (items.size - 1).coerceAtLeast(0)

            // Medidas da pílula do clima.
            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = display; textSize = unit * 1.1f }
            val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = body; textSize = unit * 0.72f }
            val wIcon = unit * 1.25f
            val pin = unit * 0.7f
            val padX = unit * 0.7f
            val sepW = 1.5f * d
            val temp = weather?.let { "${it.temp}°" } ?: ""
            val inner = if (weather == null) 0f else wIcon + gap + tempPaint.measureText(temp) + gap * 2 + sepW + gap * 2 +
                pin + gap * 0.5f + cityPaint.measureText(weather.city)
            val pillW = if (weather == null) 0f else inner + padX * 2

            val margin = unit * 0.4f
            val rowH = unit * 2.3f
            val rows = if (weather != null) 2 else 1
            val w = (maxOf(row1W, pillW) + margin * 2).toInt().coerceAtLeast(1)
            val h = (rowH * rows).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)

            // Linha 1: baterias, centralizadas.
            var x = (w - row1W) / 2f
            val cy1 = rowH / 2f
            items.forEachIndexed { i, b ->
                drawGlyph(c, b.kind.glyph, x, cy1 - icon / 2, icon)
                textPaint.color = if (b.level in 0..20) RED else INK
                c.drawText(label(b), x + icon + gap, cy1 + textPaint.textSize * 0.36f, textPaint)
                x += widths[i] + spacing
            }

            // Linha 2: pílula do clima.
            if (weather != null) {
                val cy2 = rowH * 1.5f
                val pillH = unit * 2.0f
                val left = (w - pillW) / 2f
                val rect = RectF(left, cy2 - pillH / 2, left + pillW, cy2 + pillH / 2)
                c.drawRoundRect(rect, pillH / 2, pillH / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x80000000.toInt() })
                c.drawRoundRect(
                    rect, pillH / 2, pillH / 2,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x14FFFFFF; strokeWidth = 1 * d },
                )

                var px = rect.left + padX
                drawGlyph(c, weather.glyph, px, cy2 - wIcon / 2, wIcon)
                px += wIcon + gap
                c.drawText(temp, px, cy2 + tempPaint.textSize * 0.36f, tempPaint)
                px += tempPaint.measureText(temp) + gap * 2
                c.drawRect(px, cy2 - unit * 0.45f, px + sepW, cy2 + unit * 0.45f, Paint().apply { color = 0x40FFFFFF })
                px += sepW + gap * 2
                drawGlyph(c, "pin", px, cy2 - pin / 2, pin)
                px += pin + gap * 0.5f
                c.drawText(weather.city, px, cy2 + cityPaint.textSize * 0.36f, cityPaint)
            }
            return bmp
        }

        private fun label(b: BatteryItem) = if (b.level >= 0) "${b.level}%" else "—"

        private fun drawGlyph(c: Canvas, name: String, x: Float, y: Float, size: Float) {
            val parts = Glyphs.all[name] ?: return
            val s = size / 24f
            c.save()
            c.translate(x, y)
            c.scale(s, s)
            for (part in parts) {
                val path = runCatching { PathParser.createPathFromPathData(part.d) }.getOrNull() ?: continue
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (part.red) RED else INK
                    if (part.fill) {
                        style = Paint.Style.FILL
                    } else {
                        style = Paint.Style.STROKE
                        strokeWidth = part.width
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                }
                c.drawPath(path, paint)
            }
            c.restore()
        }

        @Suppress("unused")
        private val muted = INK_MUTED + Color.TRANSPARENT
    }
}
