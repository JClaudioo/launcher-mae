package br.joao.launcher.data

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Controle do ar-condicionado Samsung pelo infravermelho do celular.
 *
 * O controle original manda o estado inteiro (ligado, modo, temperatura, ventilação...) a cada toque,
 * então o launcher guarda o último estado enviado e reenvia tudo a cada mudança. Como o ar não responde,
 * o estado mostrado é o que foi mandado por último (se alguém usar o controle de verdade, pode divergir
 * até o próximo toque aqui).
 *
 * Formato do sinal (protocolo "Samsung A/C", 38 kHz, bits do menos significativo primeiro):
 *  - mensagem normal: 2 seções de 7 bytes (ajustes com o ar já ligado);
 *  - mensagem estendida: 3 seções (ligar, desligar, timer e modo dormir) — a seção do meio é fixa.
 * Cada seção leva uma soma de verificação feita contando bits.
 */
enum class ArModo(val codigo: Int, val label: String, val glyph: String) {
    AUTO(0, "Auto", "disc"),
    FRIO(1, "Frio", "snow"),
    SECO(2, "Seco", "drop"),
    VENTILAR(3, "Ventilar", "fan"),
    QUENTE(4, "Quente", "sun"),
}

enum class ArVento(val codigo: Int, val label: String) {
    AUTO(0, "Auto"),
    BAIXA(2, "Baixa"),
    MEDIA(4, "Média"),
    ALTA(5, "Alta"),
}

data class ArEstado(
    val ligado: Boolean = false,
    val temp: Int = 23,
    val modo: ArModo = ArModo.FRIO,
    val vento: ArVento = ArVento.AUTO,
    val aletas: Boolean = false,
    /** Minutos até desligar sozinho (0 = sem timer). */
    val desligarEm: Int = 0,
    /** Hora (epoch ms) em que o timer foi mandado, para mostrar quanto falta. */
    val timerDesde: Long = 0L,
)

class ArSamsung(private val context: Context) {
    private val prefs = context.getSharedPreferences("ar", Context.MODE_PRIVATE)
    private val ir: ConsumerIrManager? = context.getSystemService(ConsumerIrManager::class.java)

    private val _estado = MutableStateFlow(carregar())
    val estado: StateFlow<ArEstado> = _estado

    /** O celular tem emissor infravermelho? */
    val disponivel: Boolean get() = ir?.hasIrEmitter() == true

    fun ligarDesligar() = aplicar(_estado.value.copy(ligado = !_estado.value.ligado), forcarEstendida = true)
    fun ligar() = aplicar(_estado.value.copy(ligado = true), forcarEstendida = true)
    fun desligar() = aplicar(_estado.value.copy(ligado = false, desligarEm = 0), forcarEstendida = true)

    fun temperatura(delta: Int) {
        val e = _estado.value
        aplicar(e.copy(temp = (e.temp + delta).coerceIn(TEMP_MIN, TEMP_MAX), ligado = true))
    }

    fun modo(m: ArModo) {
        val e = _estado.value
        // no modo Auto a ventilação é sempre automática
        aplicar(e.copy(modo = m, ligado = true, vento = if (m == ArModo.AUTO) ArVento.AUTO else e.vento))
    }

    fun vento(v: ArVento) {
        val e = _estado.value
        if (e.modo == ArModo.AUTO) return
        aplicar(e.copy(vento = v, ligado = true))
    }

    fun aletas() = aplicar(_estado.value.copy(aletas = !_estado.value.aletas, ligado = true))

    /** Desligar sozinho daqui a [minutos] (0 cancela). O ar arredonda para dezenas de minuto. */
    fun desligarEm(minutos: Int) {
        val m = (minutos.coerceIn(0, 24 * 60) / 10) * 10
        aplicar(_estado.value.copy(desligarEm = m, timerDesde = if (m > 0) System.currentTimeMillis() else 0L, ligado = true), forcarEstendida = true)
    }

    /** Minutos que ainda faltam no timer (0 = nenhum). */
    fun timerRestante(): Int {
        val e = _estado.value
        if (e.desligarEm <= 0 || !e.ligado) return 0
        val passou = ((System.currentTimeMillis() - e.timerDesde) / 60_000L).toInt()
        return (e.desligarEm - passou).coerceAtLeast(0)
    }

    private fun aplicar(novo: ArEstado, forcarEstendida: Boolean = false): Boolean {
        val anterior = _estado.value
        val estendida = forcarEstendida || novo.ligado != anterior.ligado || novo.desligarEm != anterior.desligarEm
        val ok = enviar(if (estendida) quadroEstendido(novo) else quadroNormal(novo))
        if (ok) {
            _estado.value = novo
            salvar(novo)
        }
        return ok
    }

    // ------------------------------------------------------------------ montagem dos bytes

    /** 14 bytes: seção 1 (fixa) + seção 2 com os ajustes. */
    internal fun quadroNormal(e: ArEstado): IntArray {
        val b = IntArray(14)
        secao1(b, 0)
        secaoAjustes(b, 7, e)
        soma(b, 0); soma(b, 7)
        return b
    }

    /** 21 bytes: seção 1, seção do meio (timer/dormir) e a seção de ajustes por último. */
    internal fun quadroEstendido(e: ArEstado): IntArray {
        val b = IntArray(21)
        secao1(b, 0)
        // seção do meio: 01 D2 0F 00 00 00 00 + timer de desligar
        b[7] = 0x01; b[8] = 0xD2; b[9] = 0x0F; b[10] = 0; b[11] = 0; b[12] = 0; b[13] = 0
        if (e.desligarEm > 0) {
            val dia = e.desligarEm >= 24 * 60
            if (!dia) {
                val horas = e.desligarEm / 60
                val dezMin = (e.desligarEm % 60) / 10
                b[9] = b[9] or (dezMin shl 4) or ((horas and 1) shl 7)
                b[10] = b[10] or (horas shr 1 and 0x0F)
            }
            b[12] = b[12] or (if (dia) 1 else 0) or (1 shl 2) // OffTimerEnable
        }
        secaoAjustes(b, 14, e)
        soma(b, 0); soma(b, 7); soma(b, 14)
        return b
    }

    private fun secao1(b: IntArray, o: Int) {
        b[o] = 0x02; b[o + 1] = 0x92; b[o + 2] = 0x0F; b[o + 3] = 0; b[o + 4] = 0; b[o + 5] = 0; b[o + 6] = 0xF0
    }

    /** Seção com liga/desliga, aletas, temperatura, ventilação e modo (byte a byte, bits do manual da lib). */
    private fun secaoAjustes(b: IntArray, o: Int, e: ArEstado) {
        // (valores de repouso da lib: 01 02 AE 71 00 15 F0 = 16 °C, frio, ventilação baixa, aletas, ligado)
        b[o] = 0x01
        b[o + 1] = 0x02                                                    // nibble alto = soma (entra depois)
        b[o + 2] = 0x80 or ((if (e.aletas) SWING_V else SWING_OFF) shl 4)  // nibble baixo = soma; aletas nos bits 4-6
        b[o + 3] = 0x71                                                    // visor aceso, sem turbo/eco
        b[o + 4] = ((e.temp - TEMP_MIN) and 0x0F) shl 4                    // temperatura (16 °C = 0)
        // ventilação nos bits 1-3 e modo nos bits 4-6; no modo Auto a ventilação é o código especial 6
        val vento = if (e.modo == ArModo.AUTO) 6 else e.vento.codigo
        b[o + 5] = 0x01 or (vento shl 1) or (e.modo.codigo shl 4)
        // liga/desliga aparece duas vezes: aqui (Power2) e na seção 1 (Power1): F0 ligado, C0 desligado
        b[o + 6] = if (e.ligado) 0xF0 else 0xC0
        b[6] = if (e.ligado) 0xF0 else 0xC0
    }

    /** Soma de verificação da seção que começa em [o]: conta os bits e inverte. */
    private fun soma(b: IntArray, o: Int) {
        var s = Integer.bitCount(b[o])
        s += Integer.bitCount(b[o + 1] and 0x0F)
        s += Integer.bitCount(b[o + 2] shr 4)
        for (i in 3..6) s += Integer.bitCount(b[o + i])
        val sum = s.inv() and 0xFF
        b[o + 1] = (b[o + 1] and 0x0F) or ((sum and 0x0F) shl 4)
        b[o + 2] = (b[o + 2] and 0xF0) or (sum shr 4 and 0x0F)
    }

    // ------------------------------------------------------------------ sinal

    /** Converte os bytes em tempos ligado/desligado (microssegundos) e manda pelo emissor. */
    private fun enviar(bytes: IntArray): Boolean {
        val m = ir ?: return false
        if (!m.hasIrEmitter()) return false
        val p = ArrayList<Int>(bytes.size * 16 + 8)
        p += HDR_MARK; p += HDR_SPACE
        var off = 0
        while (off < bytes.size) {
            p += SEC_MARK; p += SEC_SPACE
            for (i in off until off + 7) {
                val v = bytes[i]
                for (bit in 0 until 8) {
                    p += BIT_MARK
                    p += if ((v shr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE
                }
            }
            p += BIT_MARK; p += SEC_GAP
            off += 7
        }
        return runCatching { m.transmit(38_000, p.toIntArray()) }.isSuccess
    }

    fun padrao(bytes: IntArray): String = bytes.joinToString(" ") { "%02X".format(it) }

    private fun carregar() = ArEstado(
        ligado = prefs.getBoolean("ligado", false),
        temp = prefs.getInt("temp", 23),
        modo = runCatching { ArModo.valueOf(prefs.getString("modo", null) ?: "") }.getOrDefault(ArModo.FRIO),
        vento = runCatching { ArVento.valueOf(prefs.getString("vento", null) ?: "") }.getOrDefault(ArVento.AUTO),
        aletas = prefs.getBoolean("aletas", false),
        desligarEm = prefs.getInt("desligarEm", 0),
        timerDesde = prefs.getLong("timerDesde", 0L),
    )

    private fun salvar(e: ArEstado) {
        prefs.edit()
            .putBoolean("ligado", e.ligado).putInt("temp", e.temp)
            .putString("modo", e.modo.name).putString("vento", e.vento.name)
            .putBoolean("aletas", e.aletas).putInt("desligarEm", e.desligarEm).putLong("timerDesde", e.timerDesde)
            .apply()
    }

    companion object {
        const val TEMP_MIN = 16
        const val TEMP_MAX = 30
        private const val SWING_V = 0b010
        private const val SWING_OFF = 0b111
        private const val HDR_MARK = 690
        private const val HDR_SPACE = 17844
        private const val SEC_MARK = 3086
        private const val SEC_SPACE = 8864
        private const val SEC_GAP = 2886
        private const val BIT_MARK = 586
        private const val ONE_SPACE = 1432
        private const val ZERO_SPACE = 436
    }
}
