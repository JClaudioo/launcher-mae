package br.joao.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import br.joao.launcher.R

/**
 * Visual claro (modelo 4, 2026-11): tinta quase preta sobre papel/vidro claro, detalhe vermelho.
 * [Night] é a cor "inversa" da tinta: texto e ícones em cima de um fundo [Ink].
 */
object Palette {
    val Ink = Color(0xFF121417)
    val InkSoft = Color(0xFF2C3036)
    val InkMuted = Color(0xFF4A4F57)
    val Rule = Color(0x2E121417)
    val Red = Color(0xFFC8261B)
    /** Vermelho para texto pequeno (contraste melhor no claro). */
    val RedText = Color(0xFFB0201A)
    val Night = Color(0xFFF4F2EE)
    /** Fundo das folhas (Casa, Finanças) e diálogos. */
    val Panel = Color(0xFFF7F5F1)
    /** Fundo liso de telas inteiras (Jogos, gaveta sem papel de parede). */
    val Paper = Color(0xFFF1EFEB)
    /** Vidro: pílulas, dock, pesquisa, abas da gaveta. */
    val Glass = Color(0xA6FFFFFF)
    val GlassLine = Color(0xD9FFFFFF)
    /** Cartões brancos (baterias, Casa, ícones). */
    val Card = Color(0xE0FFFFFF)
    /** Trilho de barras de progresso. */
    val Track = Color(0x24121417)
    /** Escurecido atrás das folhas. */
    val Scrim = Color(0x66121417)
    val PillBg = Glass
    val PillInk = Ink
    val PillInkSoft = InkMuted
}

@OptIn(ExperimentalTextApi::class)
object Fonts {
    /** Títulos grandes (relógio, temperatura, porcentagens). */
    val Display = FontFamily(
        Font(R.font.archivo, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
        Font(R.font.archivo, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
    )

    /** Textos gerais. */
    val Body = FontFamily(
        Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )

    /** Títulos técnicos (dias da semana, categorias da gaveta). */
    val Tech = FontFamily(Font(R.font.chakra_petch, FontWeight.SemiBold))
}
