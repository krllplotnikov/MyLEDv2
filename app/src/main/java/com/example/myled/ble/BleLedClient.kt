package com.example.myled.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FoundDevice(val address: String, val name: String?, val rssi: Int, val isLedController: Boolean)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val name: String) : ConnectionState
}

/**
 * BLE-клиент контроллера ленты.
 *
 * Вся работа со стеком идёт на главном потоке (колбэки GATT перекладываются через [handler]),
 * поэтому очередь записи не требует синхронизации.
 *
 * Очередь записи «схлопывает» пакеты по параметру: пока предыдущая запись не подтверждена,
 * новое значение того же параметра заменяет старое. Это даёт плавное управление ползунками
 * без переполнения очереди GATT.
 *
 * Вызывающий код отвечает за наличие разрешений BLUETOOTH_SCAN / BLUETOOTH_CONNECT.
 */
@SuppressLint("MissingPermission")
class BleLedClient(context: Context) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _devices = MutableStateFlow<List<FoundDevice>>(emptyList())
    val devices: StateFlow<List<FoundDevice>> = _devices.asStateFlow()

    /** Вызывается после установки соединения и обнаружения характеристики. */
    var onReady: (() -> Unit)? = null

    private var gatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var targetAddress: String? = null
    private var userDisconnected = false

    private val pending = LinkedHashMap<Byte, ByteArray>()
    private var writeInFlight = false

    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    // ---------------------------------------------------------------- scan

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handler.post { onDeviceFound(result) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            handler.post { results.forEach(::onDeviceFound) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Scan failed: $errorCode")
            handler.post { _scanning.value = false }
        }
    }

    private val stopScanRunnable = Runnable { stopScan() }

    fun startScan(durationMs: Long = 15_000) {
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return
        if (_scanning.value) stopScan()
        _devices.value = emptyList()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            // Без фильтра: часть телефонов не находит 16-битный UUID в 128-битном поле рекламы ESP32.
            scanner.startScan(null, settings, scanCallback)
            _scanning.value = true
            handler.removeCallbacks(stopScanRunnable)
            handler.postDelayed(stopScanRunnable, durationMs)
        } catch (e: SecurityException) {
            Log.w(TAG, "No scan permission", e)
        }
    }

    fun stopScan() {
        handler.removeCallbacks(stopScanRunnable)
        if (!_scanning.value) return
        _scanning.value = false
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "stopScan", e)
        }
    }

    private fun onDeviceFound(result: ScanResult) {
        val name = result.scanRecord?.deviceName ?: result.device.name
        val uuids = result.scanRecord?.serviceUuids.orEmpty().map { it.uuid }
        val isLed = name == LedProtocol.DEVICE_NAME || LedProtocol.SERVICE_UUID in uuids
        if (name == null && !isLed) return
        val device = FoundDevice(result.device.address, name, result.rssi, isLed)
        _devices.update { list ->
            (list.filterNot { it.address == device.address } + device)
                .sortedWith(compareByDescending<FoundDevice> { it.isLedController }.thenByDescending { it.rssi })
        }
    }

    // ---------------------------------------------------------- connection

    fun connect(address: String) {
        val adapter = adapter ?: return
        if (!adapter.isEnabled) return
        stopScan()
        closeGatt()
        userDisconnected = false
        targetAddress = address
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            return
        }
        _connection.value = ConnectionState.Connecting
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        userDisconnected = true
        handler.removeCallbacks(reconnectRunnable)
        closeGatt()
        _connection.value = ConnectionState.Disconnected
    }

    private val reconnectRunnable = Runnable {
        val address = targetAddress
        if (!userDisconnected && address != null && _connection.value == ConnectionState.Disconnected) {
            connect(address)
        }
    }

    private fun closeGatt() {
        characteristic = null
        writeInFlight = false
        gatt?.let {
            try {
                it.disconnect()
                it.close()
            } catch (e: Exception) {
                Log.w(TAG, "close", e)
            }
        }
        gatt = null
    }

    fun release() {
        stopScan()
        disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            handler.post {
                if (g != gatt) return@post
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Disconnected, status=$status")
                    closeGatt()
                    _connection.value = ConnectionState.Disconnected
                    if (!userDisconnected) handler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS)
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            handler.post {
                if (g != gatt) return@post
                val ch = g.getService(LedProtocol.SERVICE_UUID)?.getCharacteristic(LedProtocol.CHARACTERISTIC_UUID)
                if (status != BluetoothGatt.GATT_SUCCESS || ch == null) {
                    Log.w(TAG, "LED service not found (status=$status)")
                    userDisconnected = true
                    closeGatt()
                    _connection.value = ConnectionState.Disconnected
                    return@post
                }
                characteristic = ch
                _connection.value = ConnectionState.Connected(g.device.name ?: g.device.address)
                onReady?.invoke()
                pump()
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) {
            handler.post {
                if (g != gatt) return@post
                if (status != BluetoothGatt.GATT_SUCCESS) Log.w(TAG, "Write failed: $status")
                writeInFlight = false
                pump()
            }
        }
    }

    // --------------------------------------------------------------- write

    /** Ставит пакет в очередь. Должен вызываться с главного потока. */
    fun send(packet: ByteArray) {
        pending.remove(packet[0])
        pending[packet[0]] = packet
        pump()
    }

    /** Сбрасывает неотправленные пакеты (например, перед полной синхронизацией). */
    fun clearQueue() = pending.clear()

    private fun pump() {
        if (writeInFlight) return
        val g = gatt ?: return
        val ch = characteristic ?: return
        val key = pending.keys.firstOrNull() ?: return
        val packet = pending.remove(key) ?: return
        val ok = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(ch, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                    BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                ch.value = packet
                @Suppress("DEPRECATION")
                g.writeCharacteristic(ch)
            }
        } catch (e: SecurityException) {
            false
        }
        if (ok) {
            writeInFlight = true
        } else {
            // Стек занят — вернём пакет в очередь и попробуем чуть позже, если новее значения нет.
            if (!pending.containsKey(key)) pending[key] = packet
            handler.postDelayed({ pump() }, RETRY_DELAY_MS)
        }
    }

    companion object {
        private const val TAG = "BleLedClient"
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val RETRY_DELAY_MS = 30L
    }
}
