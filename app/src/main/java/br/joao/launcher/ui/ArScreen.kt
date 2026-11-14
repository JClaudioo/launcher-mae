package br.joao.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.joao.launcher.data.ArEstado
import br.joao.launcher.data.ArModo
import br.joao.launcher.data.ArSamsung
import br.joao.launcher.data.ArVento
import kotlinx.coroutines.delay

class ArActions(
    val close: () -> Unit,
    val power: () -> Unit,
    val minus: () -> Unit,
    val plus: () -> Unit,
    val modo: (ArModo) -> Unit,
    val vento: (ArVento) -> Unit,
    val aletas: () -> Unit,
    /** Timer de desligar: alterna entre 0 (sem), 1 h, 2 h e 4 h. */
    val timer: () -> Unit,
)

/** Cartão compacto da tela inicial: Ligar, temperatura e − / +. Tocar fora dos botões abre o controle inteiro. */
@Composable
fun ArWidget(estado: ArEstado, disponivel: Boolean, restante: Int, open: () -> Unit, power: () -> Unit, minus: () -> Unit, plus: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Card)
            .tap("Abrir o controle do ar-condicionado", open)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph("snow", 18.dp)
            Spacer(Modifier.width(7.dp))
            Txt("AR-CONDICIONADO", 10.5.sp, family = Fonts.Tech, weight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = Palette.InkMuted, modifier = Modifier.weight(1f))
            Txt("abrir tudo", 10.5.sp, weight = FontWeight.Bold, color = Palette.InkMuted)
            Spacer(Modifier.width(2.dp))
            Glyph("chev", 14.dp, tint = Palette.InkMuted)
        }
        Spacer(Modifier.height(12.dp))
        if (!disponivel) {
            Txt("Este celular não tem o emissor infravermelho", 13.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoCartao(
                glyph = "power", label = if (estado.ligado) "Desligar" else "Ligar",
                width = 72.dp, destaque = true, onClick = power, descricao = "Ligar ou desligar o ar",
            )
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Txt("${estado.temp}°", 30.sp, family = Fonts.Display, weight = FontWeight.Black, lineHeight = 30.sp, color = if (estado.ligado) Palette.Ink else Palette.InkMuted)
                Spacer(Modifier.height(2.dp))
                Txt(subtitulo(estado, restante), 11.sp, weight = FontWeight.SemiBold, color = Palette.InkSoft, maxLines = 1)
            }
            BotaoCartao(glyph = "minus", onClick = minus, descricao = "Diminuir temperatura")
            BotaoCartao(glyph = "plus", onClick = plus, descricao = "Aumentar temperatura")
        }
    }
}

private fun subtitulo(e: ArEstado, restante: Int): String {
    if (!e.ligado) return "Desligado"
    val partes = mutableListOf(e.modo.label)
    if (e.modo != ArModo.AUTO) partes += "ventilação ${e.vento.label.lowercase()}"
    if (restante > 0) partes += "desliga em ${horas(restante)}"
    return partes.joinToString(" · ")
}

private fun horas(min: Int): String = if (min >= 60) "${min / 60} h" + (if (min % 60 > 0) " ${min % 60} min" else "") else "$min min"

@Composable
private fun BotaoCartao(glyph: String, label: String? = null, width: androidx.compose.ui.unit.Dp = 56.dp, destaque: Boolean = false, descricao: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(width)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (destaque) Palette.Red else Palette.Ink.copy(alpha = 0.07f))
            .tap(descricao, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val tint = if (destaque) Color.White else Palette.Ink
        Glyph(glyph, if (label == null) 22.dp else 20.dp, tint = tint, accent = if (destaque) Color.White else Palette.Red)
        if (label != null) {
            Spacer(Modifier.height(3.dp))
            Txt(label, 10.5.sp, weight = FontWeight.ExtraBold, color = tint)
        }
    }
}

/** Controle inteiro, numa folha que sobe de baixo. */
@Composable
fun ArSheet(ar: ArSamsung, estado: ArEstado, actions: ArActions) {
    var restante by remember { mutableIntStateOf(ar.timerRestante()) }
    LaunchedEffect(estado) {
        while (true) {
            restante = ar.timerRestante()
            delay(30_000)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.Scrim).tap("Fechar", actions.close)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 30.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Palette.Panel)
                .border(1.dp, Palette.Ink.copy(alpha = 0.08f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .tap(onClick = {}) // segura o toque para não fechar
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Ink.copy(alpha = 0.25f)))
            Spacer(Modifier.height(18.dp))
            Txt("Ar-condicionado", 30.sp, family = Fonts.Tech, weight = FontWeight.SemiBold, letterSpacing = 0.5.sp, lineHeight = 30.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glyph("remote", 14.dp)
                Spacer(Modifier.width(5.dp))
                Txt(if (ar.disponivel) "Aponte o celular para o ar" else "Este celular não tem infravermelho", 12.sp, weight = FontWeight.SemiBold, color = Palette.InkMuted)
            }

            // temperatura grande com − e +
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BotaoRedondo("minus", "Diminuir temperatura", actions.minus)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("${estado.temp}°", 84.sp, family = Fonts.Display, weight = FontWeight.Black, lineHeight = 84.sp, letterSpacing = (-3).sp, color = if (estado.ligado) Palette.Ink else Palette.InkMuted)
                    Txt(if (estado.ligado) "Temperatura" else "Desligado", 12.sp, weight = FontWeight.Bold, color = Palette.InkMuted)
                }
                BotaoRedondo("plus", "Aumentar temperatura", actions.plus)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Red).tap("Ligar ou desligar", actions.power),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph("power", 22.dp, tint = Color.White, accent = Color.White)
                Spacer(Modifier.width(10.dp))
                Txt(if (estado.ligado) "Desligar" else "Ligar", 16.sp, weight = FontWeight.ExtraBold, color = Color.White)
            }

            Spacer(Modifier.height(22.dp))
            Rotulo("MODO")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ArModo.FRIO, ArModo.VENTILAR, ArModo.SECO).forEach { m ->
                    Opcao(m.label, m.glyph, ativo = estado.modo == m, modifier = Modifier.weight(1f)) { actions.modo(m) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Rotulo("VENTILAÇÃO")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ArVento.BAIXA, ArVento.MEDIA, ArVento.ALTA, ArVento.AUTO).forEach { v ->
                    Opcao(v.label, null, ativo = estado.vento == v, alto = 50.dp, modifier = Modifier.weight(1f)) { actions.vento(v) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pilula("swing", "Aletas", ativo = estado.aletas, Modifier.weight(1f), actions.aletas)
                Pilula(
                    "timer",
                    when {
                        restante > 0 -> "Desliga em ${horas(restante)}"
                        else -> "Desligar sozinho"
                    },
                    ativo = restante > 0, Modifier.weight(1f), actions.timer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Txt("Toque em \"Desligar sozinho\" para escolher 1 h, 2 h ou 4 h.", 11.sp, weight = FontWeight.SemiBold, color = Palette.InkMuted)

            Spacer(Modifier.weight(1f))
            Txt("Fechar", 13.sp, weight = FontWeight.Bold, color = Palette.InkMuted, modifier = Modifier.align(Alignment.CenterHorizontally).tap(onClick = actions.close).padding(vertical = 14.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun Rotulo(t: String) = Txt(t, 11.sp, family = Fonts.Tech, weight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = Palette.InkMuted, modifier = Modifier.padding(start = 4.dp))

@Composable
private fun BotaoRedondo(glyph: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).shadow(8.dp, CircleShape, clip = false, ambientColor = Color(0x1A1E2024), spotColor = Color(0x1A1E2024))
            .clip(CircleShape).background(Color.White).tap(label, onClick),
        contentAlignment = Alignment.Center,
    ) { Glyph(glyph, 30.dp) }
}

@Composable
private fun Opcao(label: String, glyph: String?, ativo: Boolean, modifier: Modifier, alto: androidx.compose.ui.unit.Dp = 56.dp, onClick: () -> Unit) {
    Row(
        modifier.height(alto).clip(RoundedCornerShape(16.dp)).background(if (ativo) Palette.Ink else Palette.Ink.copy(alpha = 0.07f)).tap(label, onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            Glyph(glyph, 20.dp, tint = if (ativo) Palette.Night else Palette.Ink, accent = if (ativo) Palette.Night else Palette.Red)
            Spacer(Modifier.width(8.dp))
        }
        Txt(label, 13.5.sp, weight = FontWeight.ExtraBold, color = if (ativo) Palette.Night else Palette.Ink)
    }
}

@Composable
private fun Pilula(glyph: String, label: String, ativo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(50.dp).clip(RoundedCornerShape(25.dp)).background(if (ativo) Palette.Ink else Palette.Ink.copy(alpha = 0.06f)).tap(label, onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(glyph, 19.dp, tint = if (ativo) Palette.Night else Palette.Ink, accent = if (ativo) Palette.Night else Palette.Red)
        Spacer(Modifier.width(8.dp))
        Txt(label, 13.5.sp, weight = FontWeight.ExtraBold, color = if (ativo) Palette.Night else Palette.Ink, maxLines = 1)
    }
}
