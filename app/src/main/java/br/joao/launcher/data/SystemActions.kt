package br.joao.launcher.data

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import br.joao.launcher.R

object SystemActions {
    private const val GOOGLE = "com.google.android.googlequicksearchbox"

    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        }
    }

    fun search(context: Context) {
        Launch.intent(context, Intent("android.search.action.GLOBAL_SEARCH").setPackage(GOOGLE)) ||
            Launch.intent(context, Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "")) ||
            Launch.pkg(context, GOOGLE)
    }

    fun voiceSearch(context: Context) {
        Launch.intent(context, Intent(RecognizerIntent.ACTION_WEB_SEARCH).setPackage(GOOGLE)) ||
            Launch.intent(context, Intent(Intent.ACTION_VOICE_COMMAND)) ||
            Launch.pkg(context, GOOGLE)
    }

    /**
     * Google Lens. Tenta, nesta ordem, os jeitos que outros launchers usam para abrir a câmera do Lens
     * (dentro do app do Google e no app Lens separado). Se nenhum abrir, avisa.
     */
    fun lens(context: Context): Boolean {
        val lensUri = Uri.parse("google://lens")
        val clear = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val tries = listOf(
            Intent.makeMainActivity(android.content.ComponentName(GOOGLE, "com.google.android.apps.search.lens.LensExportedActivity"))
                .setData(lensUri).putExtra("LensHomescreenShortcut", true).addFlags(clear),
            Intent(Intent.ACTION_VIEW, lensUri).setPackage(GOOGLE).putExtra("LensHomescreenShortcut", true).addFlags(clear),
            Intent.makeMainActivity(android.content.ComponentName("com.google.ar.lens", "com.google.vr.apps.ornament.app.lens.LensLauncherActivity"))
                .addFlags(clear),
            Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens")).setPackage(GOOGLE),
        )
        for (i in tries) {
            // só tenta o que existe no celular (evita "abrir" algo que fecha na hora)
            val exists = i.component?.let { c ->
                runCatching { context.packageManager.getActivityInfo(c, 0) }.isSuccess
            } ?: (context.packageManager.queryIntentActivities(i, 0).isNotEmpty())
            if (exists && Launch.intent(context, i)) return true
        }
        return Launch.pkg(context, "com.google.ar.lens")
    }

    /** Pesquisa no app do Google (cai no navegador se não der). Usado ao tocar num jogo. */
    fun googleSearch(context: Context, query: String) {
        Launch.intent(
            context,
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query).setPackage(GOOGLE),
        ) || Launch.intent(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))),
        )
    }

    /** Claude: abre o app (a conversa nova fica a um toque). */
    fun claude(context: Context) {
        Launch.pkg(context, "com.anthropic.claude") ||
            Launch.intent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://claude.ai/new")))
    }

    fun notificationAccessSettings(context: Context) {
        Launch.intent(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun appDetails(context: Context, pkg: String) {
        Launch.intent(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }

    fun uninstall(context: Context, pkg: String) {
        Launch.intent(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
    }

    fun homeSettings(context: Context) {
        Launch.intent(context, Intent(Settings.ACTION_HOME_SETTINGS))
    }

    /** Aplica o papel de parede da árvore no sistema (tela inicial e de bloqueio). */
    fun applyWallpaper(context: Context): Boolean = runCatching {
        val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper)
        WallpaperManager.getInstance(context).setBitmap(
            bmp, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
        )
        true
    }.getOrDefault(false)
}
