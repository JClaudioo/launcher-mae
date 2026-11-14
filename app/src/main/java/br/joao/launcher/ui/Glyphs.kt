package br.joao.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ícones de linha desenhados à mão (grade 24x24): traço da tinta (escuro no visual claro) com um detalhe vermelho.
 */
class GlyphPart(val d: String, val red: Boolean = false, val fill: Boolean = false, val width: Float = 1.6f)

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0"

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r},${y}h${w - 2 * r}a$r,$r 0 0 1 $r,$r" +
        "v${h - 2 * r}a$r,$r 0 0 1 ${-r},$r" +
        "h${-(w - 2 * r)}a$r,$r 0 0 1 ${-r},${-r}" +
        "v${-(h - 2 * r)}a$r,$r 0 0 1 $r,${-r}z"

private fun p(d: String, red: Boolean = false, fill: Boolean = false, width: Float = 1.6f) =
    GlyphPart(d, red, fill, width)

object Glyphs {
    val all: Map<String, List<GlyphPart>> = mapOf(
        "alarm" to listOf(p(circle(12f, 13f, 7.5f)), p("M12 9.5V13l2.5 2", red = true), p("M4 5.5L6.5 3M20 5.5L17.5 3")),
        "tv" to listOf(p(rrect(3f, 5f, 18f, 12f, 2f)), p("M8.5 20.5h7", red = true), p("M12 17v3.5")),
        "remote" to listOf(p(rrect(8f, 2.5f, 8f, 19f, 3f)), p(circle(12f, 8f, 1.6f), red = true), p("M10.5 13h3M10.5 16h3")),
        "timer" to listOf(p(circle(12f, 13f, 8f)), p("M12 9v4l2.5 2", red = true), p("M9 2.5h6")),
        "phone" to listOf(
            p("M5 4h3l2 5-2 1.5a11 11 0 0 0 5.5 5.5L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z"),
            p("M14.5 3.5a6 6 0 0 1 6 6", red = true),
        ),
        "chat" to listOf(
            p("M12 3.5a8.5 8.5 0 0 0-7.4 12.7L3.5 20.5l4.4-1.1A8.5 8.5 0 1 0 12 3.5z"),
            p("M8.5 12h.01M12 12h.01M15.5 12h.01", red = true, width = 2.4f),
        ),
        "message" to listOf(
            p("M4 5h16v11H9l-5 4z"),
            p("M8 9.5h8", red = true),
            p("M8 12.5h5"),
        ),
        "mail" to listOf(p(rrect(3f, 5f, 18f, 14f, 2f)), p("M3 7l9 6 9-6", red = true)),
        "send" to listOf(p("M21 3L3 11l7 3 3 7z"), p("M10 14L21 3", red = true)),
        "users" to listOf(
            p(circle(9f, 8f, 3f)), p("M3 20a6 6 0 0 1 12 0"),
            p(circle(17f, 9f, 2.5f), red = true), p("M15.5 14.5A5 5 0 0 1 21 19"),
        ),
        "camsq" to listOf(
            p(rrect(3.5f, 3.5f, 17f, 17f, 5f)), p(circle(12f, 12f, 3.8f)),
            p(circle(17f, 7f, 0.9f), red = true, fill = true),
        ),
        "x" to listOf(p("M5 4.5l14 15"), p("M19 4.5L5 19.5", red = true)),
        "music" to listOf(
            p("M9 18V6l10-2v12"), p(circle(6.5f, 18f, 2.5f), red = true), p(circle(16.5f, 16f, 2.5f)),
        ),
        "briefcase" to listOf(p(rrect(3f, 7f, 18f, 13f, 2f)), p("M9 7V5h6v2"), p("M3 13h18", red = true)),
        "playc" to listOf(p(circle(12f, 12f, 9f)), p("M10 8.5l5 3.5-5 3.5z", red = true)),
        "video" to listOf(p(rrect(3f, 6f, 18f, 12f, 4f)), p("M10.5 9.5l4 2.5-4 2.5z", red = true)),
        "image" to listOf(
            p(rrect(3f, 4f, 18f, 16f, 2f)), p(circle(8.5f, 9f, 1.5f), red = true), p("M21 16l-5-5-9 9"),
        ),
        "camera" to listOf(
            p("M4 8h3l2-3h6l2 3h3v11H4z"), p(circle(12f, 13.5f, 3.5f)),
            p(circle(12f, 13.5f, 1f), red = true, fill = true),
        ),
        "film" to listOf(
            p(rrect(4f, 3f, 16f, 18f, 2f)), p("M8 3v18M16 3v18"), p("M4 12h4M16 12h4", red = true),
        ),
        "mic" to listOf(
            p(rrect(9f, 3f, 6f, 11f, 3f)), p("M5.5 11a6.5 6.5 0 0 0 13 0"), p("M12 17.5V21", red = true),
        ),
        "sheet" to listOf(p(rrect(4f, 3f, 16f, 18f, 2f)), p("M4 9h16M4 15h16"), p("M10 3v18", red = true)),
        "drive" to listOf(
            p("M7 18a4.5 4.5 0 0 1 0-9 6 6 0 0 1 11.5 2A3.5 3.5 0 0 1 18 18z"),
            p("M12 11v5M10 14l2 2 2-2", red = true),
        ),
        "calendar" to listOf(
            p(rrect(3f, 5f, 18f, 16f, 2f)), p("M3 10h18M8 3v4M16 3v4"),
            p("M15 15h.01", red = true, width = 2.6f),
        ),
        "note" to listOf(
            p("M5 3h10l4 4v14H5z"), p("M15 3v4h4"), p("M8 12h8", red = true), p("M8 16h5"),
        ),
        "spark" to listOf(p("M12 3v18M3 12h18"), p("M5.6 5.6l12.8 12.8M18.4 5.6L5.6 18.4", red = true)),
        // olho (mostrar valores) e olho riscado (esconder)
        "eye" to listOf(p("M2.5 12C5 7.5 8.3 5.5 12 5.5S19 7.5 21.5 12C19 16.5 15.7 18.5 12 18.5S5 16.5 2.5 12z"), p(circle(12f, 12f, 3f), red = true)),
        "eye_off" to listOf(
            p("M2.5 12C5 7.5 8.3 5.5 12 5.5S19 7.5 21.5 12C19 16.5 15.7 18.5 12 18.5S5 16.5 2.5 12z"), p(circle(12f, 12f, 3f)),
            p("M4 20L20 4", red = true),
        ),
        "bank" to listOf(p("M3 10l9-6 9 6", red = true), p("M5 10v8M10 10v8M14 10v8M19 10v8M3 21h18")),
        "wrench" to listOf(
            p("M15 4a5 5 0 0 0-4.6 6.9L4 17.3 6.7 20l6.4-6.4A5 5 0 0 0 20 9l-3 3-3-1-1-3z"),
            p("M6.5 17.5h.01", red = true, width = 2.4f),
        ),
        "globe" to listOf(
            p(circle(12f, 12f, 9f)), p("M3 12h18", red = true), p("M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"),
        ),
        "map" to listOf(p("M9 4l-6 2v14l6-2 6 2 6-2V4l-6 2z"), p("M9 4v14M15 6v14", red = true)),
        "clock" to listOf(p(circle(12f, 12f, 9f)), p("M12 7v5l3 2", red = true)),
        "calc" to listOf(
            p(rrect(5f, 3f, 14f, 18f, 2f)), p("M8 7h8", red = true),
            p("M8 12h.01M12 12h.01M16 12h.01M8 16h.01M12 16h.01M16 16h.01", width = 2.2f),
        ),
        "folder" to listOf(
            p("M3 6a1 1 0 0 1 1-1h5l2 2h9a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z"),
            p("M3 10h18", red = true),
        ),
        "gear" to listOf(
            p(circle(12f, 12f, 3f), red = true), p(circle(12f, 12f, 7f)),
            p("M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9L7 7M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1"),
        ),
        "store" to listOf(p("M4 8h16l-1 12H5z"), p("M9 8a3 3 0 0 1 6 0", red = true)),
        "bag" to listOf(p(rrect(4f, 7f, 16f, 13f, 3f)), p("M8 7a4 4 0 0 1 8 0"), p("M10 13.5h4", red = true)),
        "grid" to listOf(
            p(rrect(4f, 4f, 6f, 6f, 1.5f)), p(rrect(14f, 4f, 6f, 6f, 1.5f), red = true),
            p(rrect(4f, 14f, 6f, 6f, 1.5f)), p(rrect(14f, 14f, 6f, 6f, 1.5f)),
        ),
        "play" to listOf(p("M8 5.5l10.5 6.5L8 18.5z")),
        "pause" to listOf(p("M8.5 5.5v13M15.5 5.5v13")),
        "prev" to listOf(p("M18 6l-8 6 8 6zM6.5 6v12")),
        "next" to listOf(p("M6 6l8 6-8 6zM17.5 6v12")),
        "thumb" to listOf(
            p("M7 11v9H4v-9z", red = true),
            p("M7 11l4-7a2 2 0 0 1 3 2l-1 4h6a2 2 0 0 1 2 2l-2 6.5a2 2 0 0 1-2 1.5H7"),
        ),
        "search" to listOf(p(circle(11f, 11f, 6f)), p("M20 20l-4.5-4.5", red = true)),
        "lens" to listOf(
            p("M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3"),
            p(circle(12f, 12f, 3f), red = true),
        ),
        "watch" to listOf(
            p(rrect(6.5f, 6f, 11f, 12f, 3f)), p("M9 6l.8-3h4.4L15 6M9 18l.8 3h4.4L15 18"),
            p("M12 9.5V12l1.5 1", red = true),
        ),
        "device" to listOf(p(rrect(7f, 3f, 10f, 18f, 2.5f)), p("M11 18h2", red = true)),
        "buds" to listOf(
            p("M7.5 4.5a3 3 0 0 0-3 3v2a3 3 0 0 0 3 3h1v-8z"), p("M8.5 12.5V19", red = true),
            p("M16.5 4.5a3 3 0 0 1 3 3v2a3 3 0 0 1-3 3h-1v-8z"), p("M15.5 12.5V19", red = true),
        ),
        "sun" to listOf(
            p(circle(12f, 12f, 4f), red = true),
            p("M12 2.5v2M12 19.5v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2.5 12h2M19.5 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"),
        ),
        "moon" to listOf(p("M20 14A8 8 0 1 1 10 4a6 6 0 0 0 10 10z"), p("M17 4.5h.01", red = true, width = 2.4f)),
        "cloud" to listOf(p("M7 18a4.5 4.5 0 0 1 0-9 6 6 0 0 1 11.5 2A3.5 3.5 0 0 1 18 18z")),
        "cloudsun" to listOf(
            p(circle(8f, 8f, 3f), red = true),
            p("M10 19a4 4 0 0 1 0-8 5 5 0 0 1 9.6 1.6A3.2 3.2 0 0 1 19 19z"),
        ),
        "rain" to listOf(
            p("M7 14.5a4 4 0 0 1 .5-8 5 5 0 0 1 9.5 1.5 3.3 3.3 0 0 1 0 6.5z"),
            p("M9 17.5l-1 2.5M13 17.5l-1 2.5M17 17.5l-1 2.5", red = true),
        ),
        "storm" to listOf(
            p("M7 14a4 4 0 0 1 .5-8 5 5 0 0 1 9.5 1.5 3.3 3.3 0 0 1 0 6.5h-1"),
            p("M12.5 12l-2.5 4.5h3.5l-2 4.5", red = true),
        ),
        "pin" to listOf(p("M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z"), p(circle(12f, 10f, 2f), red = true)),
        "back" to listOf(p("M19 12H5M11 6l-6 6 6 6")),
        "more" to listOf(p("M12 5h.01M12 12h.01M12 19h.01", width = 2.6f)),
        "close" to listOf(p("M6 6l12 12M18 6L6 18")),
        "bolt" to listOf(p("M13 3L5 14h6l-1 7 8-11h-6z", red = true)),
        "disc" to listOf(p(circle(12f, 12f, 8.5f)), p(circle(12f, 12f, 2f), red = true), p("M12 6.5a5.5 5.5 0 0 1 5.5 5.5")),
        // Modo Casa
        "snow" to listOf(p("M12 3v18M4.5 7.5l15 9M19.5 7.5l-15 9"), p("M9.5 4.5L12 6l2.5-1.5M9.5 19.5L12 18l2.5 1.5", red = true)),
        "bulb" to listOf(p("M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"), p("M10 19h4M10.5 21.5h3", red = true)),
        "speaker" to listOf(p(rrect(6f, 3f, 12f, 18f, 3f)), p(circle(12f, 14f, 3f), red = true), p("M12 7h.01", width = 2.4f)),
        "power" to listOf(p("M12 3v8M6.3 7.5a8 8 0 1 0 11.4 0", width = 2f)),
        "minus" to listOf(p("M5 12h14")),
        "plus" to listOf(p("M5 12h14M12 5v14")),
        "chev" to listOf(p("M9 6l6 6-6 6")),
        "vol" to listOf(p("M4 9v6h4l5 4V5L8 9z"), p("M16 9a4 4 0 0 1 0 6", red = true)),
        // Controle da TV
        "up" to listOf(p("M6 15l6-6 6 6", width = 2f)),
        "down" to listOf(p("M6 9l6 6 6-6", width = 2f)),
        "left" to listOf(p("M15 6l-6 6 6 6", width = 2f)),
        "right" to listOf(p("M9 6l6 6-6 6", width = 2f)),
        "home" to listOf(p("M4 11l8-7 8 7"), p("M6 10v10h12V10"), p("M10 20v-5h4v5", red = true)),
        "menu" to listOf(p("M5 7h14M5 12h14"), p("M5 17h14", red = true)),
        "rew10" to listOf(p("M4 12a8 8 0 1 0 2.4-5.7"), p("M4 4v4.5h4.5", red = true)),
        "fwd10" to listOf(p("M20 12a8 8 0 1 1-2.4-5.7"), p("M20 4v4.5h-4.5", red = true)),
        "playpause" to listOf(p("M4 6l7 6-7 6z"), p("M15 6v12M19.5 6v12", red = true)),
        "mute" to listOf(p("M4 9v6h4l5 4V5L8 9z"), p("M16 9.5l5 5M21 9.5l-5 5", red = true)),
        "box" to listOf(p(rrect(3f, 8f, 18f, 9f, 2f)), p("M7 12.5h.01", red = true, width = 2.4f), p("M6 20h12")),
        "game" to listOf(
            p(rrect(2.5f, 7f, 19f, 11f, 5.5f)), p("M7 10.5v4M5 12.5h4"),
            p("M15.5 11.5h.01M18 13.5h.01", red = true, width = 2.4f),
        ),
        "keyboard" to listOf(
            p(rrect(2.5f, 6f, 19f, 12f, 2f)),
            p("M6 10h.01M9.5 10h.01M13 10h.01M16.5 10h.01M6 13.5h.01M18 13.5h.01M18 10h.01", width = 2f),
            p("M8.5 14.5h7", red = true),
        ),
        // Gaveta: categoria "Todos"
        "all" to listOf(
            p(circle(6f, 6f, 1.6f)), p(circle(12f, 6f, 1.6f)), p(circle(18f, 6f, 1.6f)),
            p(circle(6f, 12f, 1.6f)), p(circle(12f, 12f, 1.6f), red = true), p(circle(18f, 12f, 1.6f)),
            p(circle(6f, 18f, 1.6f)), p(circle(12f, 18f, 1.6f)), p(circle(18f, 18f, 1.6f)),
        ),
        // Atalho do Claude: balão com o asterisco vermelho
        "claude" to listOf(
            p("M12 3.5a8.5 8.5 0 0 0-7.4 12.7L3.5 20.5l4.4-1.1A8.5 8.5 0 1 0 12 3.5z"),
            p("M12 8v8M8 12h8M9.2 9.2l5.6 5.6M14.8 9.2l-5.6 5.6", red = true),
        ),
        // Sininho (seguir um jogo): apagado e ativo
        "sino" to listOf(p("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z"), p("M10 20.5a2 2 0 0 0 4 0")),
        "sino_on" to listOf(
            p("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", fill = true),
            p("M10 20.5a2 2 0 0 0 4 0"),
            p("M3.5 8.5a8 8 0 0 1 2.5-4.5M20.5 8.5a8 8 0 0 0-2.5-4.5", red = true),
        ),
        // Jogos
        "star" to listOf(p("M12 4l2.4 5 5.4.6-4 3.7 1.1 5.4L12 16l-4.9 2.7 1.1-5.4-4-3.7 5.4-.6z", red = true, fill = true)),
        "ball" to listOf(p(circle(12f, 12f, 8.5f)), p("M12 8.2l3.3 2.4-1.3 3.9h-4l-1.3-3.9z", red = true)),
        "sp_fut" to listOf(
            p(circle(12f, 12f, 8.5f)),
            p("M12 8.2l3.3 2.4-1.3 3.9h-4l-1.3-3.9z", red = true),
            p("M12 3.5v4.7M15.3 10.6l4.2-1.4M14 14.5l2.6 3.6M10 14.5l-2.6 3.6M8.7 10.6L4.5 9.2"),
        ),
        "sp_f1" to listOf(p("M5 21V4M5 4.5h13l-2.5 4 2.5 4H5"), p("M9 4.5v8M13 4.5v8M5 8.5h13", red = true)),
        "sp_bask" to listOf(
            p(circle(12f, 12f, 8.5f)), p("M3.5 12h17M12 3.5v17"),
            p("M6 6a9 9 0 0 1 0 12M18 6a9 9 0 0 0 0 12", red = true),
        ),
        "sp_volei" to listOf(
            p(circle(12f, 12f, 8.5f)),
            p("M12 3.5c-1 4 0 7 3.5 9.5M12 12c-4 .5-6.5 2.5-7.5 5.5M12 12c2.5 3 5.5 4 8.5 3.5"),
            p("M12 3.5c3.5 1.5 6 4.5 6.5 8", red = true),
        ),
        "sp_praia" to listOf(
            p(circle(12f, 9.5f, 6f)),
            p("M12 3.5c-.7 2.8 0 5 2.5 6.6M12 9.5c-2.8.4-4.5 1.8-5.3 3.9"),
            p("M3.5 19c1.4-1.2 2.8-1.2 4.2 0s2.8 1.2 4.3 0 2.8-1.2 4.2 0 2.8 1.2 4.3 0", red = true),
        ),
        "sp_mesa" to listOf(
            p("M13.5 4.5a5.5 5.5 0 0 1 0 7.8l-1.6 1.6a2 2 0 0 1-2.8 0l-3-3a2 2 0 0 1 0-2.8l1.6-1.6a5.5 5.5 0 0 1 7.8 0z"),
            p("M10.5 14.5L5 20", red = true, width = 2.2f),
            p(circle(18.5f, 17.5f, 1.8f), red = true),
        ),
        "sp_tenis" to listOf(p(circle(12f, 12f, 8.5f)), p("M5.5 6.5a8.5 8.5 0 0 1 0 11M18.5 6.5a8.5 8.5 0 0 0 0 11", red = true)),
        "sp_nfl" to listOf(
            p("M4 20C4 11 11 4 20 4c0 9-7 16-16 16z"),
            p("M9.5 14.5l5-5M10.5 11.5l2 2M12.5 9.5l2 2", red = true),
        ),
        "sp_mlb" to listOf(
            p(circle(12f, 12f, 8.5f)),
            p("M7 5.5c2 2 2 11 0 13M17 5.5c-2 2-2 11 0 13", red = true),
        ),
        // ---------------------------------------------------------- modelo 4 (2026-11): ícones dos apps
        "app" to listOf(p(rrect(5f, 5f, 14f, 14f, 4f)), p(circle(12f, 12f, 2.2f), red = true)),
        // Ar-condicionado (launcher da mãe): ventilador, gota (modo seco), aletas e o "G" do Google
        "fan" to listOf(
            p(circle(12f, 12f, 2f), red = true),
            p("M12 10a4 4 0 0 1 4-6 4 4 0 0 1-2 6zM14 12a4 4 0 0 1 6 4 4 4 0 0 1-6-2zM12 14a4 4 0 0 1-4 6 4 4 0 0 1 2-6zM10 12a4 4 0 0 1-6-4 4 4 0 0 1 6 2z"),
        ),
        "drop" to listOf(p("M12 3.5s-6 7-6 11a6 6 0 0 0 12 0c0-4-6-11-6-11z"), p("M9.5 15a2.5 2.5 0 0 0 2.5 2.5", red = true)),
        "swing" to listOf(p("M4 7h16M4 12h16M4 17h16"), p("M17 4l3 3-3 3", red = true)),
        "google" to listOf(
            p("M20 12h-8"),
            p("M20 12a8 8 0 1 1-2.4-5.7", width = 2f),
            p("M20 12a8 8 0 0 1-8 8", red = true, width = 2f),
        ),
        "bell" to listOf(p("M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"), p("M10 20.5a2 2 0 0 0 4 0", red = true)),
        "headset" to listOf(
            p("M4 14v-2a8 8 0 0 1 16 0v2"), p("M4 14h3v6H5a1 1 0 0 1-1-1zM20 14h-3v6h2a1 1 0 0 0 1-1z"),
            p("M18 20c0 1-1.5 1.5-4 1.5", red = true),
        ),
        "sticker" to listOf(p("M5 4h10l4 4v7a5 5 0 0 1-5 5H5z"), p("M15 4v4h4M9 13.5c.8 1 1.8 1.5 3 1.5", red = true)),
        "flame" to listOf(
            p("M12 3c3 3.5 6 6 6 10a6 6 0 0 1-12 0c0-2.5 1.5-4 3-5.5.3 2 1.3 3 2.5 3.5C11 8 11 5.5 12 3z"),
            p("M12 21a2.5 2.5 0 0 1-2.5-2.5c0-1.5 1.2-2.3 2.5-3.5 1.3 1.2 2.5 2 2.5 3.5A2.5 2.5 0 0 1 12 21z", red = true),
        ),
        "estrela" to listOf(
            p("M12 3l2.2 5.5L20 9l-4.5 3.8L17 19l-5-3.2L7 19l1.5-6.2L4 9l5.8-.5z"),
            p(circle(12f, 12f, 1f), red = true, fill = true),
        ),
        "tela" to listOf(p(rrect(3f, 5f, 18f, 13f, 3f)), p("M10 9l5 2.5-5 2.5z", red = true), p("M8 21h8")),
        "antenna" to listOf(
            p("M8.5 6.5a5 5 0 0 0 0 7M15.5 6.5a5 5 0 0 1 0 7M5.5 4a8.5 8.5 0 0 0 0 12M18.5 4a8.5 8.5 0 0 1 0 12M12 11.5V21"),
            p(circle(12f, 10f, 1.5f), red = true),
        ),
        "photos" to listOf(
            p(rrect(7f, 7f, 13.5f, 13f, 2.5f)), p("M4 16.5V6a2.5 2.5 0 0 1 2.5-2.5H17"), p("M7 17l3.5-3.5 3 3 2-2 5 5"),
            p(circle(16f, 11f, 1.3f), red = true),
        ),
        "ballot" to listOf(p("M4 11h16v9H4z"), p("M9 11V4h6v7"), p("M10.3 7.5l1.2 1.2 2.2-2.2", red = true)),
        "doc" to listOf(p("M6 3h8l4 4v14H6z"), p("M14 3v4h4"), p("M9 12h6M9 16h4", red = true)),
        "idcard" to listOf(
            p(rrect(3f, 5f, 18f, 14f, 2.5f)), p(circle(8.5f, 10.5f, 2f)), p("M5.8 16c.6-1.4 1.6-2 2.7-2s2.1.6 2.7 2"),
            p("M14 10h4M14 14h3", red = true),
        ),
        "chart" to listOf(p("M7 16v-5M12 16V7M17 16V8"), p("M4 20h16", red = true)),
        "card" to listOf(p(rrect(3f, 6f, 18f, 12f, 2.5f)), p("M3 10h18", red = true), p("M7 14.5h4")),
        "piggy" to listOf(
            p("M5 11.5a6 5 0 0 1 11-2.8h2.5v3l1.5 1v3h-2l-1.5 2v2.3H14v-1.5h-4V20H7.5v-2.3A5.5 5.5 0 0 1 5 11.5z"),
            p("M10 8h3", red = true),
        ),
        "wallet" to listOf(
            p("M4 7h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"), p("M4 7l11-3v3"),
            p(circle(16f, 13.5f, 1.2f), red = true),
        ),
        "trend" to listOf(p("M4 19h16"), p("M4 15l5-5 4 3 7-7"), p("M15 6h5v5", red = true)),
        "coin" to listOf(
            p(circle(12f, 12f, 8.5f)),
            p("M14.5 9.5c-.5-.9-1.5-1.5-2.5-1.5-1.4 0-2.5.8-2.5 2s1.1 1.7 2.5 2 2.5.8 2.5 2-1.1 2-2.5 2c-1 0-2-.6-2.5-1.5M12 6.5V8M12 16v1.5", red = true),
        ),
        "cards" to listOf(
            p(rrect(3f, 8f, 15f, 11f, 2f)), p("M6 8V6a1 1 0 0 1 1-1h13a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-2"),
            p("M3 12h15", red = true),
        ),
        "parcel" to listOf(p("M4 8l8-4 8 4v8l-8 4-8-4z"), p("M4 8l8 4 8-4M12 12v8"), p("M8 6l8 4", red = true)),
        "hanger" to listOf(
            p("M12 9L3.5 15a1.5 1.5 0 0 0 .9 2.7h15.2a1.5 1.5 0 0 0 .9-2.7L12 9V7.5"),
            p("M12 7.5a2 2 0 1 1 2-2", red = true),
        ),
        "tag" to listOf(p("M3.5 12.5V4h8.5l8.5 8.5-8.5 8.5z"), p(circle(8f, 8f, 1.5f), red = true)),
        "cart" to listOf(
            p("M3 4h2.5l2.2 10.5h10.3L20 7.5H7"),
            p(circle(9.5f, 18.5f, 1.5f), red = true), p(circle(16.5f, 18.5f, 1.5f), red = true),
        ),
        "plane" to listOf(
            p("M12 3c.9 0 1.5.9 1.5 2v4.5l7 4v2l-7-2V18l2 1.5V21L12 20l-3.5 1v-1.5l2-1.5v-4.5l-7 2v-2l7-4V5c0-1.1.6-2 1.5-2z"),
            p("M4.5 13.5l6-3.5M19.5 13.5l-6-3.5", red = true),
        ),
        "ticket" to listOf(p("M3 6h18v4a2 2 0 0 0 0 4v4H3v-4a2 2 0 0 0 0-4z"), p("M14.5 6v2M14.5 11v2M14.5 16v2", red = true)),
        "compass" to listOf(p(circle(12f, 12f, 8.5f)), p("M15.5 8.5l-2 5-5 2 2-5z", red = true)),
        "route" to listOf(
            p("M6 17V9a3 3 0 0 1 6 0v6a3 3 0 0 0 6 0V9"),
            p(circle(6f, 19f, 2f), red = true), p(circle(18f, 7f, 2f), red = true),
        ),
        "car" to listOf(
            p("M4.5 16v-4.5L6.8 6.5h10.4l2.3 5V16z"), p("M4.5 11.5h15"),
            p(circle(8f, 16f, 1.8f), red = true), p(circle(16f, 16f, 1.8f), red = true),
        ),
        "shield" to listOf(p("M12 3l8 3v6c0 4.5-3.5 7.5-8 9-4.5-1.5-8-4.5-8-9V6z"), p("M9 12l2 2 4-4", red = true)),
        "target" to listOf(p(circle(12f, 12f, 8.5f)), p(circle(12f, 12f, 4.5f)), p(circle(12f, 12f, 1f), red = true, fill = true)),
        "hub" to listOf(
            p(circle(5f, 6f, 1.8f)), p(circle(19f, 6f, 1.8f)), p(circle(5f, 18f, 1.8f)), p(circle(19f, 18f, 1.8f)),
            p("M6.4 7.2l3.8 3.4M17.6 7.2l-3.8 3.4M6.4 16.8l3.8-3.4M17.6 16.8l-3.8-3.4"),
            p(circle(12f, 12f, 2.5f), red = true),
        ),
        "plug" to listOf(p("M9 3v5M15 3v5"), p("M6 8h12v3a6 6 0 0 1-12 0z"), p("M12 17v4", red = true)),
        "wifi" to listOf(
            p("M3 9.5a13 13 0 0 1 18 0M6 13a8.5 8.5 0 0 1 12 0M9 16.5a4 4 0 0 1 6 0"),
            p(circle(12f, 19.5f, 0.8f), red = true, fill = true),
        ),
        "solar" to listOf(p("M4 14l2-8h12l2 8z"), p("M5 10h14M10 6l-1 8M14 6l1 8"), p("M12 14v4M8 20h8", red = true)),
        "heart" to listOf(
            p("M12 20s-7.5-4.5-7.5-10A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 7.5 3c0 5.5-7.5 10-7.5 10z"),
            p("M7 12h2.5l1.5-2.5 2 4 1.5-1.5H17", red = true),
        ),
        "dumbbell" to listOf(p("M6.5 8v8M17.5 8v8M3.5 10v4M20.5 10v4"), p("M6.5 12h11", red = true)),
        "pulse" to listOf(p("M7 12l2-5 4 10 2-5h6"), p("M3 12h4", red = true)),
        "aperture" to listOf(
            p(circle(12f, 12f, 8.5f)),
            p("M12 3.5l3.5 6M20.5 12h-7M16 19.5l-3.5-6M8 19.5l3.5-6M3.5 12h7M8 4.5l3.5 6", red = true),
        ),
        "galaxystore" to listOf(
            p("M4 9h16l-1.5 11h-13z"), p("M8 9V7a4 4 0 0 1 8 0v2"),
            p("M9.5 14.5c.8 1 1.6 1.5 2.5 1.5s1.7-.5 2.5-1.5", red = true),
        ),
        "lock" to listOf(p(rrect(5f, 10f, 14f, 10f, 2.5f)), p("M8 10V7a4 4 0 0 1 8 0v3"), p(circle(12f, 15f, 1.3f), red = true)),
        "sliders" to listOf(p("M4 7h10M18 7h2M4 17h4M12 17h8"), p(circle(16f, 7f, 2f), red = true), p(circle(10f, 17f, 2f), red = true)),
        "navbar" to listOf(
            p(rrect(3f, 14.5f, 18f, 6f, 3f)), p("M12 3.5v7M9 7.5l3 3 3-3"),
            p("M8 17.5h.01M12 17.5h.01M16 17.5h.01", red = true, width = 2.4f),
        ),
        "notif" to listOf(
            p("M6 16v-5a6 6 0 0 1 9-5.2M18 11v5l1.5 2h-15"), p("M10 20.5a2 2 0 0 0 4 0"),
            p(circle(18f, 6f, 2.2f), red = true, fill = true),
        ),
        "palette" to listOf(
            p("M12 3.5a8.5 8.5 0 1 0 0 17c1.2 0 1.5-.8 1.5-1.5 0-1.5-1.5-1.8-1.5-3s1-1.5 2-1.5h2.5A4 4 0 0 0 20.5 11 8 7.5 0 0 0 12 3.5z"),
            p(circle(8f, 11f, 1f), red = true), p(circle(11f, 7.5f, 1f), red = true), p(circle(15f, 8f, 1f), red = true),
        ),
    )

    private val cache = HashMap<String, Path>()

    fun path(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }
}

@Composable
fun Glyph(
    name: String,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
    tint: Color = Palette.Ink,
    accent: Color = Palette.Red,
) {
    val parts = remember(name) { Glyphs.all[name] ?: Glyphs.all.getValue("grid") }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            for (part in parts) {
                val color = if (part.red) accent else tint
                val path = Glyphs.path(part.d)
                if (part.fill) {
                    drawPath(path, color, style = Fill)
                } else {
                    drawPath(
                        path, color,
                        style = Stroke(width = part.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
