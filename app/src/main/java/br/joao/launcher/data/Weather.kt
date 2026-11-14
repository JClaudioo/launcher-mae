package br.joao.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

data class DayForecast(val label: String, val glyph: String, val max: Int, val min: Int)

data class WeatherState(
    val city: String,
    val temp: Int,
    val glyph: String,
    val note: String,
    val days: List<DayForecast>,
)

/** Previsão do Open-Meteo (gratuito, sem cadastro) para uma cidade fixa — sem pedir localização. */
class WeatherRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("clima", Context.MODE_PRIVATE)
    private val ptBR = Locale("pt", "BR")

    fun isStale(maxAgeMs: Long = 20 * 60 * 1000L): Boolean =
        System.currentTimeMillis() - prefs.getLong("at", 0L) > maxAgeMs

    fun cached(): WeatherState? = prefs.getString("json", null)?.let { runCatching { parse(JSONObject(it)) }.getOrNull() }

    suspend fun refresh(): WeatherState? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
                    "&current=temperature_2m,weather_code,is_day" +
                    "&hourly=precipitation_probability,weather_code" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=America%2FSao_Paulo&forecast_days=5",
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(body)
            val state = parse(json)
            prefs.edit().putString("json", body).putLong("at", System.currentTimeMillis()).apply()
            state
        }.getOrNull()
    }

    private fun parse(json: JSONObject): WeatherState {
        val current = json.getJSONObject("current")
        val code = current.getInt("weather_code")
        val isDay = current.optInt("is_day", 1) == 1
        val temp = Math.round(current.getDouble("temperature_2m")).toInt()

        val daily = json.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxs = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val days = (0 until minOf(4, dates.length())).map { i ->
            val date = LocalDate.parse(dates.getString(i))
            val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, ptBR)
                .replace(".", "").uppercase(ptBR).take(3)
            DayForecast(label, glyphFor(codes.getInt(i), true), Math.round(maxs.getDouble(i)).toInt(), Math.round(mins.getDouble(i)).toInt())
        }

        return WeatherState(
            city = CITY,
            temp = temp,
            glyph = glyphFor(code, isDay),
            note = rainNote(json, code),
            days = days,
        )
    }

    /** "Chuva até as 16h" / "Chuva por volta das 19h" / "Sem chuva hoje". */
    private fun rainNote(json: JSONObject, currentCode: Int): String {
        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val probs = hourly.getJSONArray("precipitation_probability")
        val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
        var start = 0
        while (start < times.length() && LocalDateTime.parse(times.getString(start)).isBefore(now)) start++
        val end = minOf(times.length(), start + 12)
        if (isRain(currentCode)) {
            for (i in start until end) {
                if (probs.optInt(i, 0) < 30) {
                    return "Chuva até as ${LocalDateTime.parse(times.getString(i)).hour}h"
                }
            }
            return "Chuva nas próximas horas"
        }
        for (i in start until end) {
            if (probs.optInt(i, 0) >= 50) {
                return "Chuva por volta das ${LocalDateTime.parse(times.getString(i)).hour}h"
            }
        }
        return "Sem chuva prevista"
    }

    private fun isRain(code: Int) = code in 51..67 || code in 80..82 || code >= 95

    companion object {
        const val CITY = "Ceres"
        const val LAT = -15.3075
        const val LON = -49.5994

        fun glyphFor(code: Int, isDay: Boolean): String = when (code) {
            0 -> if (isDay) "sun" else "moon"
            1, 2 -> if (isDay) "cloudsun" else "moon"
            3, 45, 48 -> "cloud"
            in 51..67, in 80..82 -> "rain"
            in 71..77, 85, 86 -> "cloud"
            in 95..99 -> "storm"
            else -> "cloud"
        }
    }
}
