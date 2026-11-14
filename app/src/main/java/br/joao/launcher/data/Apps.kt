package br.joao.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Um app da gaveta. No launcher da mãe o ícone é o original do app (colorido, do jeito que ela já conhece),
 * só desenhado numa pastilha arredondada para a grade ficar uniforme.
 */
data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: ImageBitmap?,
)

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val iconCache = HashMap<String, ImageBitmap?>()

    fun load(): List<AppEntry> {
        val list = launcherApps.getActivityList(null, Process.myUserHandle())
        return list
            .filter { it.applicationInfo.packageName != context.packageName }
            .map { info ->
                val key = info.componentName.flattenToString()
                AppEntry(
                    label = info.label?.toString() ?: info.applicationInfo.packageName,
                    packageName = info.applicationInfo.packageName,
                    component = info.componentName,
                    icon = iconCache.getOrPut(key) {
                        runCatching {
                            val d = info.getIcon(DENSITY) ?: return@runCatching null
                            val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
                            d.setBounds(0, 0, SIZE, SIZE)
                            d.draw(Canvas(bmp))
                            bmp.asImageBitmap()
                        }.getOrNull()
                    },
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private companion object {
        const val SIZE = 192
        /** 0 = densidade do aparelho. */
        const val DENSITY = 0
    }
}

object Launch {
    fun app(context: Context, entry: AppEntry) {
        runCatching {
            context.getSystemService(LauncherApps::class.java)
                .startMainActivity(entry.component, Process.myUserHandle(), null, null)
        }.onFailure { pkg(context, entry.packageName) }
    }

    fun pkg(context: Context, vararg packages: String): Boolean {
        for (p in packages) {
            val intent = context.packageManager.getLaunchIntentForPackage(p) ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    fun intent(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    fun isInstalled(context: Context, pkg: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(pkg) != null
}
