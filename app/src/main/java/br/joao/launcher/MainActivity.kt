@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package br.joao.launcher

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import br.joao.launcher.data.AppEntry
import br.joao.launcher.data.AppRepository
import br.joao.launcher.data.ArModo
import br.joao.launcher.data.ArSamsung
import br.joao.launcher.data.ArVento
import br.joao.launcher.data.BatteryMonitor
import br.joao.launcher.data.Launch
import br.joao.launcher.data.SystemActions
import br.joao.launcher.data.WeatherRepository
import br.joao.launcher.data.WeatherState
import br.joao.launcher.media.MediaMonitor
import br.joao.launcher.ui.ArActions
import br.joao.launcher.ui.ArSheet
import br.joao.launcher.ui.DockItem
import br.joao.launcher.ui.DrawerActions
import br.joao.launcher.ui.DrawerScreen
import br.joao.launcher.ui.HomeActions
import br.joao.launcher.ui.HomeScreen
import br.joao.launcher.widget.LockWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    private lateinit var appsRepo: AppRepository
    private lateinit var batteries: BatteryMonitor
    private lateinit var media: MediaMonitor
    private lateinit var weatherRepo: WeatherRepository
    private lateinit var ar: ArSamsung

    private var drawerOpen by mutableStateOf(false)
    /** Tela visível (entre onResume e onPause): só aí a troca de página é animada. */
    private var resumed = false
    /** Voltou de tela apagada/outro app: vai direto para a tela inicial, sem animação. */
    private var snapHome = false
    private var arOpen by mutableStateOf(false)
    private var apps by mutableStateOf<List<AppEntry>>(emptyList())
    private var weather by mutableStateOf<WeatherState?>(null)
    private var mediaAccess by mutableStateOf(false)
    private var dock by mutableStateOf<List<DockItem>>(emptyList())
    private var lastWeather = 0L

    private val btPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        batteries.restart()
    }

    /** Primeira abertura: Bluetooth (baterias do relógio e dos fones) e notificações. */
    private val startPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        batteries.restart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            // visual claro: ícones escuros na barra de status e na de navegação
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val app = application as LauncherApp
        appsRepo = AppRepository(this)
        batteries = app.batteries
        media = MediaMonitor(this)
        weatherRepo = app.weather
        weather = weatherRepo.cached()
        ar = app.ar

        val missing = listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) startPermissions.launch(missing.toTypedArray())

        setContent {
            var now by remember { mutableStateOf(LocalDateTime.now()) }
            LaunchedEffect(Unit) {
                while (true) {
                    now = LocalDateTime.now()
                    delay(1000L - (System.currentTimeMillis() % 1000L))
                }
            }
            val battery by batteries.items.collectAsState()
            val playing by media.state.collectAsState()
            val arState by ar.estado.collectAsState()
            // quanto falta no timer de desligar (confere de minuto em minuto)
            var arRestante by remember { mutableStateOf(ar.timerRestante()) }
            LaunchedEffect(arState) {
                while (true) {
                    arRestante = ar.timerRestante()
                    delay(60_000)
                }
            }

            BackHandler(enabled = drawerOpen) { drawerOpen = false }
            BackHandler(enabled = arOpen) { arOpen = false }

            Box(Modifier.fillMaxSize()) {
                Image(
                    painterResource(R.drawable.wallpaper_claro),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Duas páginas: tela inicial (0) e todos os apps (1), trocadas deslizando para o lado
                val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
                LaunchedEffect(drawerOpen) {
                    val target = if (drawerOpen) 1 else 0
                    val snap = snapHome || !resumed
                    snapHome = false
                    if (pager.currentPage != target) {
                        if (snap) pager.scrollToPage(target) else pager.animateScrollToPage(target)
                    }
                }
                LaunchedEffect(pager) {
                    androidx.compose.runtime.snapshotFlow { pager.settledPage }.collect { drawerOpen = it == 1 }
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    if (page == 0) {
                        HomeScreen(
                            now = now,
                            weather = weather,
                            music = playing,
                            hasMediaAccess = mediaAccess,
                            batteries = battery,
                            ar = arState,
                            arDisponivel = ar.disponivel,
                            arRestante = arRestante,
                            dock = dock,
                            actions = homeActions(),
                        )
                    } else {
                        DrawerScreen(apps, drawerActions(), visible = pager.currentPage == 1)
                    }
                }
                // Controle inteiro do ar: sobe de baixo ao tocar no cartão
                AnimatedVisibility(
                    visible = arOpen,
                    enter = slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(180)),
                ) {
                    ArSheet(ar, arState, arActions())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        apps = appsRepo.load()
        dock = dockItems()
        batteries.start()
        mediaAccess = media.hasAccess()
        media.start()
        refreshWeather()
    }

    override fun onPause() {
        super.onPause()
        // tela apagada ou outro app por cima: na volta, sempre a tela inicial
        resumed = false
        snapHome = true
        drawerOpen = false
        arOpen = false
    }

    override fun onDestroy() {
        media.stop()
        super.onDestroy()
    }

    /** Botão "home" com o launcher já aberto: volta para a tela inicial. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen = false
        arOpen = false
    }

    private fun refreshWeather() {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastWeather < 20 * 60 * 1000L && weather != null) return
        lastWeather = nowMs
        lifecycleScope.launch {
            weatherRepo.refresh()?.let {
                weather = it
                LockWidget.updateAll(this@MainActivity)
            }
        }
    }

    /**
     * Atalhos de baixo. São os apps que ela mais usa; o primeiro de cada lista que estiver
     * instalado é o que aparece (o celular é Xiaomi, mas os nomes da Samsung ficam de reserva).
     */
    private fun dockItems(): List<DockItem> {
        val candidates = listOf(
            Triple("Telefone", "phone", listOf("com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer")),
            Triple("WhatsApp", "chat", listOf("com.whatsapp", "com.whatsapp.w4b")),
            Triple("Câmera", "camera", listOf("com.android.camera", "com.google.android.GoogleCamera", "com.sec.android.app.camera")),
            Triple("Galeria", "photos", listOf("com.miui.gallery", "com.google.android.apps.photos", "com.sec.android.gallery3d")),
            Triple("Mensagens", "message", listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging")),
        )
        return candidates.mapNotNull { (label, glyph, pkgs) ->
            val pkg = pkgs.firstOrNull { Launch.isInstalled(this, it) } ?: return@mapNotNull null
            DockItem(label, glyph) { Launch.pkg(this, pkg) }
        }
    }

    private fun homeActions() = HomeActions(
        playPause = { media.playPause() },
        next = { media.next() },
        previous = { media.previous() },
        like = { media.like() },
        openMusic = {
            val pkg = media.openApp()
            if (pkg == null || !Launch.pkg(this, pkg)) {
                Launch.pkg(this, "com.google.android.apps.youtube.music", "com.miui.player", "com.spotify.music")
            }
        },
        grantMedia = { grantMediaAccess() },
        search = { SystemActions.search(this) },
        voice = { SystemActions.voiceSearch(this) },
        lens = { if (!SystemActions.lens(this)) toast("Não consegui abrir o Google Lens neste celular") },
        openDrawer = { drawerOpen = true },
        notifications = { SystemActions.expandNotifications(this) },
        openWeather = { Launch.pkg(this, "com.miui.weather2", "com.google.android.googlequicksearchbox") },
        google = { if (!Launch.pkg(this, "com.google.android.googlequicksearchbox")) SystemActions.search(this) },
        openAr = { arOpen = true },
        arPower = { arSend { ar.ligarDesligar() } },
        arMinus = { arSend { ar.temperatura(-1) } },
        arPlus = { arSend { ar.temperatura(1) } },
    )

    // ------------------------------------------------------------------ Ar-condicionado (infravermelho)

    private fun arActions() = ArActions(
        close = { arOpen = false },
        power = { arSend { ar.ligarDesligar() } },
        minus = { arSend { ar.temperatura(-1) } },
        plus = { arSend { ar.temperatura(1) } },
        modo = { m: ArModo -> arSend { ar.modo(m) } },
        vento = { v: ArVento -> arSend { ar.vento(v) } },
        aletas = { arSend { ar.aletas() } },
        timer = { escolherTimer() },
    )

    /** Manda o comando pelo infravermelho e vibra de leve; avisa se o celular não tiver emissor. */
    private fun arSend(acao: () -> Unit) {
        if (!ar.disponivel) {
            toast("Este celular não tem emissor infravermelho")
            return
        }
        acao()
        window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** "Desligar sozinho": 1 h, 2 h, 4 h ou cancelar. */
    private fun escolherTimer() {
        val opcoes = arrayOf("Daqui a 1 hora", "Daqui a 2 horas", "Daqui a 4 horas", "Não desligar sozinho")
        val minutos = intArrayOf(60, 120, 240, 0)
        AlertDialog.Builder(this)
            .setTitle("Desligar o ar sozinho")
            .setItems(opcoes) { _, i ->
                arSend { ar.desligarEm(minutos[i]) }
                toast(if (minutos[i] > 0) "O ar desliga em ${opcoes[i].removePrefix("Daqui a ")}" else "Timer cancelado")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun drawerActions() = DrawerActions(
        close = { drawerOpen = false },
        open = { Launch.app(this, it) },
        details = { SystemActions.appDetails(this, it.packageName) },
        uninstall = { SystemActions.uninstall(this, it.packageName) },
        musicAccess = { grantMediaAccess() },
        bluetoothAccess = {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                toast("O acesso ao Bluetooth já está liberado")
            } else {
                btPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        },
        applyWallpaper = {
            lifecycleScope.launch {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    SystemActions.applyWallpaper(this@MainActivity)
                }
                toast(if (ok) "Papel de parede aplicado" else "Não foi possível aplicar o papel de parede")
            }
        },
        defaultLauncher = { SystemActions.homeSettings(this) },
        lastError = { showLastError() },
    )

    /** Último fechamento com erro: mostra o texto e deixa copiar para mandar. */
    private fun showLastError() {
        val file = java.io.File(filesDir, "ultimo-erro.txt")
        if (!file.exists()) {
            toast("Nenhum erro registrado")
            return
        }
        val text = file.readText()
        val view = android.widget.TextView(this).apply {
            setText(text)
            setTextIsSelectable(true)
            textSize = 11f
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Último erro")
            .setView(android.widget.ScrollView(this).apply { addView(view) })
            .setPositiveButton("Copiar") { _, _ ->
                val cm = getSystemService(android.content.ClipboardManager::class.java)
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Erro do launcher", text))
                toast("Erro copiado")
            }
            .setNeutralButton("Apagar") { _, _ -> file.delete(); toast("Registro apagado") }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun grantMediaAccess() {
        toast("Ative \"Launcher – música\" na lista")
        SystemActions.notificationAccessSettings(this)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    @Suppress("unused")
    private val transparent = Color.Transparent
}
