package br.joao.launcher.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class DeviceKind(val label: String, val glyph: String) {
    PHONE("Celular", "device"),
    WATCH("Relógio", "watch"),
    BUDS("Fones", "buds"),
}

/** level = -1 quando o aparelho não informa a bateria. */
data class BatteryItem(val kind: DeviceKind, val level: Int, val charging: Boolean = false, val address: String = "")

/**
 * Bateria do celular + relógio/fones conectados por Bluetooth.
 * Relógio e fones só aparecem enquanto estiverem conectados.
 */
class BatteryMonitor(private val context: Context) {
    private val _items = MutableStateFlow(listOf(BatteryItem(DeviceKind.PHONE, -1)))
    val items: StateFlow<List<BatteryItem>> = _items

    private var phone = BatteryItem(DeviceKind.PHONE, -1)
    private val bt = LinkedHashMap<String, BatteryItem>()
    private var registered = false
    private val gattTried = HashMap<String, Long>()
    private val main = Handler(Looper.getMainLooper())

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    phone = BatteryItem(
                        DeviceKind.PHONE,
                        if (level >= 0 && scale > 0) level * 100 / scale else -1,
                        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                    )
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> device(intent)?.let { add(it, batteryOf(it)) }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> device(intent)?.let { bt.remove(it.address) }
                ACTION_BATTERY_LEVEL_CHANGED -> device(intent)?.let {
                    val lvl = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (bt.containsKey(it.address) || isConnected(it)) add(it, lvl)
                }
            }
            publish()
        }
    }

    fun start() {
        if (registered) {
            scanConnected(); publish(); return
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (hasBtPermission()) {
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            filter.addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        registered = true
        scanConnected()
        publish()
    }

    fun restart() {
        stop(); start()
    }

    fun stop() {
        if (registered) runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    private fun publish() {
        val watch = bt.values.lastOrNull { it.kind == DeviceKind.WATCH }
        val buds = bt.values.lastOrNull { it.kind == DeviceKind.BUDS }
        _items.value = listOfNotNull(phone, watch, buds)
    }

    private fun hasBtPermission() =
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun scanConnected() {
        if (!hasBtPermission()) return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        runCatching {
            bt.clear()
            adapter.bondedDevices?.forEach { d -> if (isConnected(d)) add(d, batteryOf(d)) }
            // Relógios como Amazfit/Zepp ficam conectados por Bluetooth de baixo consumo (GATT).
            context.getSystemService(BluetoothManager::class.java)
                ?.getConnectedDevices(BluetoothProfile.GATT)
                ?.forEach { d -> if (!bt.containsKey(d.address)) add(d, batteryOf(d)) }
        }
    }

    @Suppress("DEPRECATION")
    private fun device(intent: Intent): BluetoothDevice? =
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    @SuppressLint("MissingPermission")
    private fun add(d: BluetoothDevice, level: Int) {
        val kind = kindOf(d) ?: return
        val previous = bt[d.address]
        val lvl = if (level >= 0) level else previous?.level ?: -1
        bt.remove(d.address)
        bt[d.address] = BatteryItem(kind, lvl, address = d.address)
        if (lvl < 0 && kind == DeviceKind.WATCH) readGattBattery(d)
    }

    /**
     * Muitos relógios (Amazfit, por exemplo) não repassam a bateria ao Android.
     * Tenta ler direto do relógio pelo serviço padrão de bateria do Bluetooth (0x180F).
     */
    @SuppressLint("MissingPermission")
    private fun readGattBattery(d: BluetoothDevice) {
        val now = SystemClock.elapsedRealtime()
        if (now - (gattTried[d.address] ?: 0L) < 10 * 60 * 1000L) return
        gattTried[d.address] = now
        runCatching {
            d.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                        BluetoothProfile.STATE_DISCONNECTED -> g.close()
                    }
                }

                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    val ch = g.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL)
                    if (ch == null || !g.readCharacteristic(ch)) g.disconnect()
                }

                override fun onCharacteristicRead(
                    g: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int,
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS && value.isNotEmpty()) {
                        val level = value[0].toInt() and 0xFF
                        main.post {
                            bt[d.address]?.let { bt[d.address] = it.copy(level = level.coerceIn(0, 100)) }
                            publish()
                        }
                    }
                    g.disconnect()
                }
            }, BluetoothDevice.TRANSPORT_LE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun kindOf(d: BluetoothDevice): DeviceKind? {
        val name = runCatching { d.name ?: "" }.getOrDefault("").lowercase()
        val major = runCatching { d.bluetoothClass?.majorDeviceClass }.getOrNull()
        return when {
            major == BluetoothClass.Device.Major.WEARABLE -> DeviceKind.WATCH
            WATCH_NAMES.any { name.contains(it) } -> DeviceKind.WATCH
            major == BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceKind.BUDS
            listOf("bud", "pods", "fone", "headset", "earphone", "jbl", "airdots", "freebuds").any { name.contains(it) } ->
                DeviceKind.BUDS
            else -> null
        }
    }

    private fun isConnected(d: BluetoothDevice): Boolean =
        runCatching { d.javaClass.getMethod("isConnected").invoke(d) as Boolean }.getOrDefault(false)

    private fun batteryOf(d: BluetoothDevice): Int =
        runCatching { d.javaClass.getMethod("getBatteryLevel").invoke(d) as Int }.getOrDefault(-1)

    companion object {
        private val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private val WATCH_NAMES = listOf(
            "watch", "relógio", "relogio", "band", "amazfit", "gts", "gtr", "bip", "zepp", "huami",
            "garmin", "forerunner", "venu", "fitbit", "versa", "polar", "suunto", "haylou", "colmi",
        )
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
