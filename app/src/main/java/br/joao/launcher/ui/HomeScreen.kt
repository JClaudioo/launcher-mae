package br.joao.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.joao.launcher.data.ArEstado
import br.joao.launcher.data.BatteryItem
import br.joao.launcher.data.WeatherState
import br.joao.launcher.media.NowPlaying
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DockItem(val label: String, val glyph: String, val onClick: () -> Unit)

class HomeActions(
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val like: () -> Unit,
    val openMusic: () -> Unit,
    val grantMedia: () -> Unit,
    val search: () -> Unit,
    val voice: () -> Unit,
    val lens: () -> Unit,
    val openDrawer: () -> Unit,
    val notifications: () -> Unit,
    val openWeather: () -> Unit,
    /** Atalho do Google ao lado da pesquisa. */
    val google: () -> Unit,
    /** Cartão do ar-condicionado: abrir o controle inteiro e os três botões rápidos. */
    val openAr: () -> Unit,
    val arPower: () -> Unit,
    val arMinus: () -> Unit,
    val arPlus: () -> Unit,
)

private val ptBR = Locale("pt", "BR")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", ptBR)
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)

@Composable
fun Txt(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    family: FontFamily = Fonts.Body,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Palette.Ink,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = family,
        fontWeight = weight,
        fontSize = size,
        color = color,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Clique sem o efeito de "ondinha" do Material. */
fun Modifier.tap(label: String? = null, onClick: () -> Unit): Modifier = this
    .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
    .clickable(interactionSource = null, indication = null, onClick = onClick)

@Composable
fun HomeScreen(
    now: LocalDateTime,
    weather: WeatherState?,
    music: NowPlaying?,
    hasMediaAccess: Boolean,
    batteries: List<BatteryItem>,
    ar: ArEstado,
    arDisponivel: Boolean,
    arRestante: Int,
    dock: List<DockItem>,
    actions: HomeActions,
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val threshold = 60.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -threshold) actions.openDrawer()
                        else if (total > threshold) actions.notifications()
                    },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            },
    ) {
        // Véu claro no alto: deixa o relógio e a música legíveis em cima da foto
        Box(
            Modifier.fillMaxWidth().height(420.dp).background(
                Brush.verticalGradient(
                    0f to Color(0xD1F1EFEB),
                    0.62f to Color(0xA8F1EFEB),
                    1f to Color(0x00F1EFEB),
                ),
            ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 14.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.padding(horizontal = 10.dp)) { ClockWeather(now, weather, actions.openWeather) }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.padding(horizontal = 10.dp)) { MusicPlayer(music, hasMediaAccess, actions) }
            Spacer(Modifier.height(20.dp))
            // Baterias em cartões (dividem a largura entre os aparelhos conectados) e, embaixo, o ar-condicionado
            Batteries(batteries)
            Spacer(Modifier.height(8.dp))
            ArWidget(ar, arDisponivel, arRestante, actions.openAr, actions.arPower, actions.arMinus, actions.arPlus, Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            PageDots(current = 0, onOther = actions.openDrawer)
            Spacer(Modifier.height(8.dp))
            // Google e pesquisa em cima, dock de vidro por último
            Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Palette.Glass)
                        .border(1.dp, Palette.GlassLine, CircleShape)
                        .tap("Abrir o Google", actions.google),
                    contentAlignment = Alignment.Center,
                ) { Glyph("google", 24.dp) }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) { SearchPill(actions) }
            }
            Spacer(Modifier.height(12.dp))
            Dock(dock)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ClockWeather(now: LocalDateTime, weather: WeatherState?, onWeather: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Txt(
                    now.format(timeFmt), 68.sp, family = Fonts.Display, weight = FontWeight.Black,
                    lineHeight = 68.sp, letterSpacing = (-2).sp,
                )
                Spacer(Modifier.height(4.dp))
                Txt(
                    now.format(dateFmt).replaceFirstChar { it.titlecase(ptBR) }, 13.sp,
                    weight = FontWeight.Bold, color = Palette.InkSoft,
                )
            }
            if (weather != null) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 4.dp).tap("Clima", onWeather)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Glyph(weather.glyph, 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Txt("${weather.temp}°", 34.sp, family = Fonts.Display, weight = FontWeight.Black, lineHeight = 34.sp)
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Glyph("pin", 12.dp)
                        Spacer(Modifier.width(3.dp))
                        Txt(weather.city, 11.5.sp, weight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(2.dp))
                    // só o clima de agora (sem a previsão dos próximos dias); a máxima e a mínima de hoje vão na frase
                    val hoje = weather.days.firstOrNull()
                    Txt(
                        if (hoje != null) "${weather.note} · ${hoje.max}°/${hoje.min}°" else weather.note,
                        11.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicPlayer(music: NowPlaying?, hasAccess: Boolean, actions: HomeActions) {
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(music) {
        while (true) {
            position = music?.positionNow() ?: 0L
            if (music?.playing != true) break
            delay(500)
        }
    }
    val playing = music?.playing == true

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(Palette.Ink))
        Spacer(Modifier.height(16.dp))
        // Capa do álbum, título e artista ao lado
        Row(verticalAlignment = Alignment.CenterVertically) {
            val artShape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .size(78.dp)
                    .shadow(10.dp, artShape, clip = false, ambientColor = Color(0x331E2024), spotColor = Color(0x331E2024))
                    .clip(artShape)
                    .background(Palette.Card)
                    .tap(if (music == null) "Abrir o app de música" else "Abrir ${music.app}") {
                        if (!hasAccess) actions.grantMedia() else actions.openMusic()
                    },
                contentAlignment = Alignment.Center,
            ) {
                val art = music?.art
                if (art != null) {
                    androidx.compose.foundation.Image(
                        art, null, Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Glyph("music", 30.dp, tint = Palette.InkMuted)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                when {
                    !hasAccess -> {
                        Txt("Toque para mostrar a música aqui", 16.sp, weight = FontWeight.ExtraBold, modifier = Modifier.tap(onClick = actions.grantMedia))
                        Txt("Permita o acesso a notificações do Launcher", 12.5.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft)
                    }
                    music == null -> {
                        Txt("Nada tocando", 17.sp, weight = FontWeight.ExtraBold)
                        Txt("Toque na capa para abrir o YouTube Music", 12.5.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft)
                    }
                    else -> {
                        Txt(music.title, 17.sp, weight = FontWeight.ExtraBold, maxLines = 2, modifier = Modifier.tap(onClick = actions.openMusic))
                        Spacer(Modifier.height(3.dp))
                        Txt(
                            listOf(music.artist, music.app).filter { it.isNotBlank() }.joinToString(" · "),
                            12.5.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft, maxLines = 1,
                        )
                    }
                }
            }
        }
        if (music != null && hasAccess) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(fmt(position), 11.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Track)) {
                    val frac = if (music.durationMs > 0) (position.toFloat() / music.durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(frac).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Red))
                }
                Spacer(Modifier.width(10.dp))
                Txt(if (music.durationMs > 0) fmt(music.durationMs) else "--:--", 11.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).tap("Curtir", actions.like), contentAlignment = Alignment.CenterStart) {
                    Glyph("thumb", 20.dp, tint = if (music.canLike) Palette.Ink else Palette.InkMuted.copy(alpha = 0.4f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButtonish("prev", "Anterior", actions.previous)
                    Spacer(Modifier.width(8.dp))
                    // tocar/pausar: círculo escuro com o desenho claro
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Palette.Ink).tap("Tocar ou pausar", actions.playPause),
                        contentAlignment = Alignment.Center,
                    ) { Glyph(if (playing) "pause" else "play", 20.dp, tint = Palette.Night) }
                    Spacer(Modifier.width(8.dp))
                    IconButtonish("next", "Próxima", actions.next)
                }
            }
        }
    }
}

@Composable
private fun IconButtonish(glyph: String, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).tap(label, onClick), contentAlignment = Alignment.Center) {
        Glyph(glyph, 20.dp)
    }
}

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Um cartão branco por aparelho conectado; os cartões dividem a largura (só o celular = largura toda). */
@Composable
private fun Batteries(items: List<BatteryItem>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { b ->
            val low = b.level in 0..20
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Palette.Card)
                    .padding(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Glyph(b.kind.glyph, 20.dp)
                    Spacer(Modifier.weight(1f))
                    Txt(
                        if (b.level >= 0) "${b.level}%" else "—", 19.sp, family = Fonts.Display, weight = FontWeight.Black,
                        color = if (low) Palette.RedText else Palette.Ink, maxLines = 1,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Track)) {
                    if (b.level >= 0) {
                        Box(
                            Modifier.fillMaxWidth(b.level / 100f).height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (low) Palette.Red else Palette.Ink),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Txt(
                    if (b.charging) "${b.kind.label} · carregando" else b.kind.label, 11.sp,
                    weight = FontWeight.Bold, color = Palette.InkSoft, maxLines = 1,
                )
            }
        }
    }
}

/** Dock (modelo 4B): uma barra de vidro com os atalhos, sem pastilha em cada ícone. */
@Composable
private fun Dock(items: List<DockItem>) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassLine, shape)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            Box(Modifier.size(52.dp).tap(item.label, item.onClick), contentAlignment = Alignment.Center) {
                Glyph(item.glyph, 26.dp)
            }
        }
    }
}

/** Pontinhos das páginas (tela inicial e apps): o vermelho comprido é a página atual; tocar leva à outra. */
@Composable
fun PageDots(current: Int, onOther: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(20.dp).tap(if (current == 0) "Ir para a página de apps" else "Voltar para a tela inicial", onOther),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0..1) {
            if (i > 0) Spacer(Modifier.width(6.dp))
            Box(
                Modifier.width(if (i == current) 16.dp else 6.dp).height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i == current) Palette.Red else Palette.Ink.copy(alpha = 0.4f)),
            )
        }
    }
}

@Composable
private fun SearchPill(actions: HomeActions) {
    val shape = RoundedCornerShape(27.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassLine, shape)
            .tap("Pesquisar", actions.search)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph("search", 20.dp)
        Spacer(Modifier.width(10.dp))
        Txt("Pesquisar", 14.sp, weight = FontWeight.Bold, color = Palette.InkMuted, modifier = Modifier.weight(1f))
        Box(Modifier.size(40.dp, 44.dp).tap("Pesquisa por voz", actions.voice), contentAlignment = Alignment.Center) {
            Glyph("mic", 20.dp)
        }
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Palette.Ink).tap("Pesquisar com a câmera", actions.lens),
            contentAlignment = Alignment.Center,
        ) { Glyph("lens", 18.dp, tint = Palette.Night) }
    }
}
