package br.joao.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.joao.launcher.data.AppEntry

class DrawerActions(
    val close: () -> Unit,
    val open: (AppEntry) -> Unit,
    val details: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
    val musicAccess: () -> Unit,
    val bluetoothAccess: () -> Unit,
    val applyWallpaper: () -> Unit,
    val defaultLauncher: () -> Unit,
    /** Mostra (e copia) o motivo do último fechamento com erro. */
    val lastError: () -> Unit,
)

@Composable
fun DrawerScreen(apps: List<AppEntry>, actions: DrawerActions, visible: Boolean = true) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppEntry?>(null) }

    // Saiu da página de apps: apaga a pesquisa e fecha o teclado (não fica por cima da tela inicial)
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(visible) {
        if (!visible) {
            searching = false
            query = ""
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }
    val closeOnOverscroll = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) {
                    pulled += available.y
                    if (pulled > threshold) {
                        pulled = 0f; actions.close()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }

    val shown = if (searching && query.isNotBlank()) {
        apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    } else {
        apps
    }

    // Página de apps (ao lado da tela inicial): o papel de parede aparece atrás de um véu claro
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xD1F1EFEB)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
                .padding(horizontal = 14.dp),
        ) {
            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (searching) {
                    SearchField(query, { query = it }, Modifier.weight(1f))
                    Box(
                        Modifier.size(44.dp).tap("Fechar pesquisa") { searching = false; query = "" },
                        contentAlignment = Alignment.Center,
                    ) { Glyph("close", 20.dp) }
                } else {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                        Txt("Apps", 30.sp, family = Fonts.Tech, weight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                        Spacer(Modifier.width(10.dp))
                        Txt("${shown.size} apps", 12.sp, weight = FontWeight.SemiBold, color = Palette.InkMuted, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Box(Modifier.size(44.dp).tap("Pesquisar apps") { searching = true }, contentAlignment = Alignment.Center) {
                        Glyph("search", 20.dp)
                    }
                    Box(Modifier.size(44.dp).tap("Mais opções") { menuOpen = true }, contentAlignment = Alignment.Center) {
                        Glyph("more", 20.dp)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                // a grade ocupa a tela toda e rola quando não cabe
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(shown, key = { it.component.flattenToString() }) { app ->
                    AppCell(app, onClick = { actions.open(app) }, onLongClick = { selected = app })
                }
            }

            Spacer(Modifier.height(16.dp))
            PageDots(current = 1, onOther = actions.close)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (menuOpen) {
        OptionsDialog(
            title = "Opções",
            onDismiss = { menuOpen = false },
            options = listOf(
                "Mostrar a música (acesso a notificações)" to actions.musicAccess,
                "Baterias do relógio e fones (Bluetooth)" to actions.bluetoothAccess,
                "Aplicar papel de parede (início e bloqueio)" to actions.applyWallpaper,
                "Escolher o launcher padrão" to actions.defaultLauncher,
                "Último erro (para mandar ao Claude)" to actions.lastError,
            ),
        )
    }

    selected?.let { app ->
        OptionsDialog(
            title = app.label,
            onDismiss = { selected = null },
            options = listOf(
                "Informações do app" to { actions.details(app) },
                "Desinstalar" to { actions.uninstall(app) },
            ),
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Txt("Pesquisar apps", 22.sp, family = Fonts.Tech, weight = FontWeight.SemiBold, color = Palette.InkMuted)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(Palette.Red),
            textStyle = TextStyle(color = Palette.Ink, fontSize = 22.sp, fontFamily = Fonts.Tech, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(app: AppEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, 60.dp)
        Spacer(Modifier.height(7.dp))
        Text(
            app.label, fontFamily = Fonts.Body, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 15.sp,
            color = Palette.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** Ícone original do app, do jeito que ela já conhece, com os cantos arredondados. */
@Composable
fun AppIcon(app: AppEntry, size: Dp) {
    val shape = RoundedCornerShape(size * 0.27f)
    Box(
        Modifier
            .size(size)
            .shadow(6.dp, shape, clip = false, ambientColor = Color(0x2E1E2024), spotColor = Color(0x2E1E2024))
            .clip(shape)
            .background(Palette.Card),
        contentAlignment = Alignment.Center,
    ) {
        val icon = app.icon
        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(size))
        else Glyph("app", size * 0.6f)
    }
}

/** Pastilha branca redonda dos ícones; [redDot] põe o ponto vermelho no canto. */
@Composable
fun Pastilha(diameter: Dp, redDot: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(diameter)
            .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0x2E1E2024), spotColor = Color(0x2E1E2024))
            .clip(CircleShape)
            .background(Palette.Card)
            .border(1.dp, Palette.Ink.copy(alpha = 0.06f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (redDot) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(bottom = diameter * 0.1f, end = diameter * 0.1f)
                    .size(diameter * 0.14f).clip(CircleShape).background(Palette.Red),
            )
        }
    }
}

@Composable
private fun OptionsDialog(title: String, onDismiss: () -> Unit, options: List<Pair<String, () -> Unit>>) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF7F5F1))
                .border(1.dp, Color(0x14121417), RoundedCornerShape(24.dp))
                .padding(vertical = 14.dp),
        ) {
            Txt(title, 18.sp, family = Fonts.Tech, weight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
            Box(Modifier.padding(horizontal = 22.dp).width(26.dp).height(3.dp).background(Palette.Red))
            Spacer(Modifier.height(8.dp))
            options.forEach { (label, action) ->
                Txt(
                    label, 15.sp, weight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tap { onDismiss(); action() }
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                )
            }
            Txt(
                "Cancelar", 14.sp, color = Palette.InkMuted,
                modifier = Modifier.fillMaxWidth().tap(onClick = onDismiss).padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
}

