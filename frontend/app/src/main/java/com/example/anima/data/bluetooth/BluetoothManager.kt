package com.example.anima.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.anima.data.model.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.charset.Charset
import java.util.*
class BluetoothManager(
    private val context: Context,
    private val parser: BluetoothParser,
    private val deviceNameFilter: String = "AnimaSmartGlasses"
) {
    companion object {
        private const val TAG = "BluetoothManager"

        val SERVICE_UUID: UUID = UUID.fromString("7f9c5eed-5678-47ca-9aa7-7337b8096792")
        val CHAR_UUID: UUID = UUID.fromString("a22db1ad-2575-4108-9b46-43feea464ae7")
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    // Simple state enum
    enum class State { Idle, Scanning, Connecting, Connected, Disconnected, Error }

    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    // Using SharedFlow for sensor data is correct for multiple collectors
    private val _sensorFlow = MutableSharedFlow<SensorData>(replay = 1)
    val sensorFlow: SharedFlow<SensorData> = _sensorFlow.asSharedFlow()

    private val _rawFlow = MutableSharedFlow<String>(replay = 1)
    val rawFlow: SharedFlow<String> = _rawFlow.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    private val adapter: BluetoothAdapter? = btManager.adapter
    private var scanner: BluetoothLeScanner? = adapter?.bluetoothLeScanner
    private var scanning = false

    private var bluetoothGatt: BluetoothGatt? = null
    private val scanResults = mutableMapOf<String, ScanResult>()

    // ------------------------------------------------------------------------
    // Scan logic
    // ------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun startScan(timeoutMs: Long = 8000L) {
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth adapter unavailable or disabled")
            _state.tryEmit(State.Error)
            return
        }

        if (!hasScanPermission()) {
            Log.w(TAG, "Missing Bluetooth scan permissions")
            _state.tryEmit(State.Error)
            return
        }

        _state.tryEmit(State.Scanning)
        scanResults.clear()

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner = adapter.bluetoothLeScanner
        scanner?.startScan(filters, settings, scanCallback)
        scanning = true

        // Stop after timeout and auto-connect
        scope.launch {
            kotlinx.coroutines.delay(timeoutMs)
            if (scanning) {
                stopScan()
                // Auto-connect to the best device found, or try connecting to the device by name if not found via UUID filter
                scanResults.values.firstOrNull()?.device?.let { device ->
                    connectToDevice(device)
                } ?: Log.d(TAG, "No devices found by filter.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (scanning) {
            scanner?.stopScan(scanCallback)
        }
        scanning = false
        _state.tryEmit(State.Idle)
    }

    // ------------------------------------------------------------------------
    // Connection logic
    // ------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        _state.tryEmit(State.Connecting)
        bluetoothGatt?.close()
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "Error disconnecting GATT", t)
        } finally {
            bluetoothGatt = null
            _state.tryEmit(State.Disconnected)
        }
    }

    fun close() {
        disconnect()
        scope.coroutineContext.cancel()
    }

    // ------------------------------------------------------------------------
    // Scan callback
    // ------------------------------------------------------------------------
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name
            val addr = result.device.address
            scanResults[addr] = result

            Log.d(TAG, "Found device name=$name addr=$addr")

            // Auto-connect by name as a fallback/primary method
            if (!name.isNullOrEmpty() && name.contains(deviceNameFilter, ignoreCase = true)) {
                stopScan()
                connectToDevice(result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Scan failed: $errorCode")
            _state.tryEmit(State.Error)
        }
    }

    // ------------------------------------------------------------------------
    // GATT callback
    // ------------------------------------------------------------------------
    private val gattCallback = object : BluetoothGattCallback() {
        // ... (connection logic remains the same)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected — discovering services")
                    bluetoothGatt = gatt
                    _state.tryEmit(State.Connected)
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from device")
                    _state.tryEmit(State.Disconnected)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Service discovery failed: $status")
                _state.tryEmit(State.Error)
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(CHAR_UUID)

            if (characteristic == null) {
                Log.w(TAG, "Characteristic not found")
                _state.tryEmit(State.Error)
                return
            }

            // Enable notifications locally
            gatt.setCharacteristicNotification(characteristic, true)

            // Write to the CCCD descriptor on the device
            val cccd = characteristic.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
            } else {
                Log.w(TAG, "CCCD missing; notifications may not work")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d(TAG, "Descriptor write status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.tryEmit(State.Connected)
            } else {
                _state.tryEmit(State.Error)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHAR_UUID) {
                val bytes = characteristic.value
                val s = bytes.toString(Charset.forName("UTF-8"))
                scope.launch {
                    _rawFlow.emit(s)
                    // 👈 Call the injected parser
                    val parsed = parser.parseSensorString(s)
                    _sensorFlow.emit(parsed)
                }
            }
        }
    }

    // ... (rest of the file remains the same)
    private fun hasScanPermission(): Boolean { /* ... */ return true }
}