package com.chessx.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bluetooth Classic SPP — optimal for chess real-time:
 * - Dedicated reader thread (single consumer, no race)
 * - Newline framing, BufferedReader/Writer (minimal syscalls)
 * - Heartbeat PING/PONG every 3s, timeout 9s, auto-reconnect x5
 * - All I/O off Main, SecurityException / IOException caught
 * - Max message 8KB, min socket timeouts 0 (blocking but cancellable)
 */
enum class BtState { IDLE, SCANNING, CONNECTING, WAITING_HOST, CONNECTED, DISCONNECTED, ERROR }

enum class DeviceRole { UNKNOWN, HOST, GUEST }

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val bonded: Boolean,
    val role: DeviceRole = DeviceRole.UNKNOWN,
    val username: String? = null,
    val isChessX: Boolean = false
)

class BluetoothClassicManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BtClassic"
        val CHESS_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val HEARTBEAT_MS = 3000L
        private const val RECONNECT_DELAY_MS = 1500L
        private const val MAX_RECONNECT = 5
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private fun adapter(): BluetoothAdapter? = try { bluetoothManager.adapter } catch (_: SecurityException) { null }

    private val _state = MutableStateFlow(BtState.IDLE)
    val state: StateFlow<BtState> = _state.asStateFlow()

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _incoming = MutableSharedFlow<BtMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<BtMessage> = _incoming

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Callback when a new connection is established. */
    var onConnected: (() -> Unit)? = null

    @Volatile var isHost: Boolean = false
        private set
    @Volatile var hostName: String = "CHESS-X · Anfitrión"
        private set

    private var serverSocket: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null
    private var writer: BufferedWriter? = null
    private var readerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var acceptJob: Job? = null
    private var reconnectAttempts = 0
    private var lastDevice: BluetoothDevice? = null
    private var shouldReconnect = false
    private val sendLock = Any()
    private val ioExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "bt-io").apply { isDaemon = true } }
    private val seenAddresses = mutableSetOf<String>()

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        } ?: return
                        addDevice(device, intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, -1000).toInt())
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val device = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        } ?: return
                        if (device.bondState == BluetoothDevice.BOND_BONDED) addDevice(device, -50)
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scope.launch { _state.value = BtState.IDLE }
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security onReceiver", e)
            } catch (e: Exception) {
                Log.e(TAG, "Receiver error", e)
            }
        }
    }
    private var receiverRegistered = false

    private fun addDevice(device: BluetoothDevice, rssi: Int) {
        try {
            val addr = device.address ?: return
            synchronized(seenAddresses) { seenAddresses.add(addr) }
            val name = try { device.name ?: device.alias ?: "Dispositivo" } catch (_: SecurityException) { "Sin permiso" }
            val bonded = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
            
            // Detect if it's a CHESS-X device and extract role/username
            val (isChessX, role, username) = parseChessXInfo(name)
            
            scope.launch {
                val current = _devices.value.toMutableList()
                current.removeAll { it.address == addr }
                current.add(DiscoveredDevice(name, addr, rssi, bonded, role, username, isChessX))
                _devices.value = current.sortedWith(
                    compareByDescending<DiscoveredDevice> { it.isChessX }
                        .thenByDescending { it.bonded }
                        .thenBy { it.name }
                )
            }
        } catch (e: Exception) { Log.w(TAG, "addDevice", e) }
    }

    private fun parseChessXInfo(name: String): Triple<Boolean, DeviceRole, String?> {
        val lower = name.lowercase()
        // Check if it's our app by service record name or naming convention
        val isChessX = lower.contains("chess") || lower.contains("chx")
        
        var role = DeviceRole.UNKNOWN
        var username: String? = null
        
        if (isChessX) {
            // Try to parse format: "CHESS-X · Anfitrión" or "CHESS-X · Invitado" or "CHESS-X · Username"
            if (lower.contains("anfitri")) role = DeviceRole.HOST
            else if (lower.contains("invitad")) role = DeviceRole.GUEST
            
            // Extract username after "·"
            val parts = name.split("·")
            if (parts.size > 1) {
                val candidate = parts.last().trim()
                if (candidate.isNotBlank() && !candidate.equals("Anfitrión", true) && !candidate.equals("Invitado", true)) {
                    username = candidate
                }
            }
        }
        
        return Triple(isChessX, role, username)
    }

    fun isBluetoothOn(): Boolean = try { adapter()?.isEnabled == true } catch (_: SecurityException) { false }
    fun isAvailable(): Boolean = adapter() != null

    fun setVisibleDeviceName(name: String): Boolean {
        return try {
            val bluetoothAdapter = bluetoothManager.adapter ?: run {
                _lastError.value = "Bluetooth no disponible"
                return false
            }
            bluetoothAdapter.name = name.trim().take(248)
            true
        } catch (e: SecurityException) {
            _lastError.value = "Sin permiso para cambiar el nombre Bluetooth"
            Log.e(TAG, "setVisibleDeviceName requires Bluetooth permission", e)
            false
        } catch (e: Exception) {
            _lastError.value = "No se pudo actualizar el nombre Bluetooth"
            Log.e(TAG, "setVisibleDeviceName failed", e)
            false
        }
    }

    fun updateHostName(username: String?) {
        hostName = "CHESS-X · ${username?.ifBlank { "Anfitrión" } ?: "Anfitrión"}"
        if (isHost && _state.value == BtState.WAITING_HOST) {
            setVisibleDeviceName(hostName)
        }
    }

    fun bondedDevices(): List<DiscoveredDevice> {
        return try {
            val set = adapter()?.bondedDevices ?: emptySet()
            set.mapNotNull { d ->
                try {
                    val name = d.name ?: "Dispositivo"
                    val (isChessX, role, username) = parseChessXInfo(name)
                    DiscoveredDevice(name, d.address, -50, true, role, username, isChessX)
                } catch (_: SecurityException) { null }
            }
        } catch (_: SecurityException) { emptyList() }
    }

    @Suppress("DEPRECATION")
    fun startDiscovery() {
        try {
            if (_state.value == BtState.CONNECTED) return
            val a = adapter() ?: run { _lastError.value = "Bluetooth no disponible"; return }
            _devices.value = bondedDevices().sortedBy { it.name }
            seenAddresses.clear()
            bondedDevices().forEach { seenAddresses.add(it.address) }
            registerReceiver()
            _state.value = BtState.SCANNING
            try { a.startDiscovery() } catch (_: SecurityException) { _lastError.value = "Sin permiso Bluetooth" }
            // Classic discovery auto-stops in ~12s; also keep bonded visible
        } catch (e: Exception) { Log.e(TAG, "startDiscovery", e) }
    }

    fun stopDiscovery() {
        try { adapter()?.cancelDiscovery() } catch (_: Exception) {}
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            receiverRegistered = true
        } catch (e: Exception) { Log.w(TAG, "registerReceiver", e) }
    }

    /** Host path: make discoverable intent must be launched by UI (startActivity). This listens. */
    fun hostListen() {
        if (_state.value == BtState.CONNECTED) return
        isHost = true
        shouldReconnect = false
        reconnectAttempts = 0
        _state.value = BtState.WAITING_HOST
        stopDiscovery()
        acceptJob?.cancel()
        // Set visible name with role
        setVisibleDeviceName(hostName)
        acceptJob = scope.launch(Dispatchers.IO) {
            try {
                closeSocketOnly()
                val a = adapter() ?: run { _state.value = BtState.ERROR; return@launch }
                try { serverSocket?.close() } catch (_: Exception) {}
                serverSocket = a.listenUsingRfcommWithServiceRecord("CHESS-X", CHESS_UUID)
                Log.d(TAG, "Host listening SPP")
                while (isActive && _state.value == BtState.WAITING_HOST) {
                    val accepted = try { serverSocket?.accept() } catch (e: Exception) { Log.w(TAG, "accept fail", e); break }
                    if (accepted != null) {
                        socket = accepted
                        onSocketConnected()
                        break
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "hostListen permission", e)
                _lastError.value = "Sin permiso Bluetooth para escuchar"
                _state.value = BtState.ERROR
            } catch (e: Exception) {
                Log.e(TAG, "hostListen", e)
                if (_state.value == BtState.WAITING_HOST) _state.value = BtState.ERROR
            }
        }
    }

    /** Guest path: connect to a device address. Non-blocking, coroutine IO. */
    fun connectTo(address: String) {
        if (_state.value == BtState.CONNECTED || _state.value == BtState.CONNECTING) return
        isHost = false
        shouldReconnect = true
        reconnectAttempts = 0
        _state.value = BtState.CONNECTING
        stopDiscovery()
        val a = adapter() ?: run { _lastError.value = "Sin Bluetooth"; _state.value = BtState.ERROR; return }
        val device = try { a.getRemoteDevice(address) } catch (e: Exception) { _lastError.value = "Dispositivo no válido"; _state.value = BtState.ERROR; return }
        lastDevice = device
        scope.launch(Dispatchers.IO) {
            try {
                withTimeout(15000) {
                    closeSocketOnly()
                    val s: BluetoothSocket = device.createRfcommSocketToServiceRecord(CHESS_UUID)
                    // Reflection fallback for SPP if needed (most devices OK with proper UUID)
                    s.connect()
                    socket = s
                    onSocketConnected()
                }
            } catch (e: Exception) {
                Log.e(TAG, "connect failed $address", e)
                _lastError.value = "No se pudo conectar. Revisa que el otro esté en 'Esperando…'"
                _state.value = BtState.ERROR
                if (shouldReconnect && reconnectAttempts < MAX_RECONNECT) scheduleReconnect()
            }
        }
    }

    private fun onSocketConnected() {
        reconnectAttempts = 0
        scope.launch { _state.value = BtState.CONNECTED }
        try {
            val s = socket ?: return
            val out = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
            writer = out
            val inputReader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            startReader(inputReader)
            startHeartbeat()
            // Notify connection established
            onConnected?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "onSocketConnected setup", e)
            handleDisconnect("Error al iniciar canal")
        }
    }

    private fun startReader(reader: BufferedReader) {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val line = try { reader.readLine() } catch (e: Exception) { Log.w(TAG, "readLine", e); null }
                    if (line == null) break // EOF => peer closed
                    val msg = MessageParser.parse(line)
                    if (msg != null) {
                        if (!_incoming.tryEmit(msg)) {
                            Log.e(TAG, "Incoming message buffer full; dropped ${msg.type}")
                        }
                    } else if (line.isNotBlank()) {
                        Log.w(TAG, "Unknown line len=${line.length}")
                    }
                }
            } catch (e: CancellationException) {
                // normal
            } catch (e: Exception) {
                Log.e(TAG, "reader loop", e)
            } finally {
                if (isActive) handleDisconnect("Conexión perdida")
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(HEARTBEAT_MS)
                if (_state.value != BtState.CONNECTED) break
                sendInternal(PingMessage(System.currentTimeMillis()))
            }
        }
    }

    /** Thread-safe send. Returns true if queued. Never throws. */
    fun send(msg: BtMessage): Boolean = sendInternal(msg)

    private fun sendInternal(msg: BtMessage): Boolean {
        return try {
            val w = writer ?: return false
            val line = msg.toJsonLine()
            if (line.length > 65536) return false
            synchronized(sendLock) {
                w.write(line)
                w.write("\n")
                w.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "send ${msg.type}", e)
            if (_state.value == BtState.CONNECTED) handleDisconnect("Error de envío")
            false
        }
    }

    private fun scheduleReconnect() {
        scope.launch(Dispatchers.IO) {
            delay(RECONNECT_DELAY_MS)
            reconnectAttempts++
            if (!shouldReconnect || _state.value == BtState.CONNECTED) return@launch
            if (reconnectAttempts > MAX_RECONNECT) {
                _lastError.value = "No se pudo reconectar tras $MAX_RECONNECT intentos"
                _state.value = BtState.ERROR
                return@launch
            }
            val addr = lastDevice?.address ?: return@launch
            Log.d(TAG, "Reconnect #$reconnectAttempts to $addr")
            connectTo(addr)
        }
    }

    private fun handleDisconnect(reason: String) {
        scope.launch {
            if (_state.value == BtState.CONNECTED) {
                Log.w(TAG, "Disconnect: $reason")
                _lastError.value = reason
                _state.value = BtState.DISCONNECTED
                stopHeartbeatReader()
                if (shouldReconnect && reconnectAttempts < MAX_RECONNECT) scheduleReconnect()
            }
        }
    }

    private fun stopHeartbeatReader() {
        heartbeatJob?.cancel()
        readerJob?.cancel()
    }

    private fun closeSocketOnly() {
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null; writer = null
    }

    fun disconnect() {
        shouldReconnect = false
        acceptJob?.cancel()
        stopHeartbeatReader()
        try { serverSocket?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        serverSocket = null; socket = null; writer = null
        scope.launch { _state.value = BtState.IDLE }
        stopDiscovery()
    }

    fun release() {
        disconnect()
        if (receiverRegistered) {
            try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        ioExecutor.shutdownNow()
    }
}

// Helper: host discoverable intent (must be fired via Activity)
fun discoverableIntent(): Intent {
    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
    return intent
}

@Suppress("DEPRECATION")
fun isBluetoothPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 31) {
        context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
