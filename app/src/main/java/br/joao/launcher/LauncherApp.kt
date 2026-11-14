package br.joao.launcher

import android.app.Application
import br.joao.launcher.data.BatteryMonitor
import br.joao.launcher.data.WeatherRepository
import br.joao.launcher.widget.LockWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Mantém o monitor de baterias vivo enquanto o processo do launcher existir,
 * para o widget da tela de bloqueio se atualizar mesmo com o launcher em segundo plano.
 */
class LauncherApp : Application() {
    /**
     * Se o launcher fechar com erro, guarda o motivo em "ultimo-erro.txt" antes de fechar.
     * Dá para ver e copiar em Gaveta › Mais opções › Último erro.
     */
    private fun guardarErros() {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->
            runCatching {
                val versao = packageManager.getPackageInfo(packageName, 0).versionName
                val quando = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())
                java.io.File(filesDir, "ultimo-erro.txt").writeText(
                    "Launcher da mãe $versao · $quando\n\n" + android.util.Log.getStackTraceString(erro),
                )
            }
            anterior?.uncaughtException(thread, erro)
        }
    }

    lateinit var batteries: BatteryMonitor
        private set
    lateinit var weather: WeatherRepository
        private set

    /** Ar-condicionado Samsung pelo infravermelho (estado guardado entre aberturas). */
    lateinit var ar: br.joao.launcher.data.ArSamsung
        private set

    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        guardarErros()
        batteries = BatteryMonitor(this)
        weather = WeatherRepository(this)
        ar = br.joao.launcher.data.ArSamsung(this)
        batteries.start()
        scope.launch {
            batteries.items.collect { LockWidget.updateAll(this@LauncherApp) }
        }
    }
}
