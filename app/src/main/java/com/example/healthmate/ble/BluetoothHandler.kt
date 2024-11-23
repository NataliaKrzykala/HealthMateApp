package com.example.healthmate.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import com.example.healthmate.ui.convertTimestampToByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.Continuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.os.Handler
import android.os.Looper


class BluetoothHandler(
    private val activity: Activity,
    private val activityResultRegistry: ActivityResultRegistry,
    private val onScanResult: () -> Unit
) {

    //region Variables
    private var btPermission = false
    var bluetoothGatt: BluetoothGatt? = null

    private var onServicesDiscovered: ((List<BluetoothGattService>) -> Unit)? = null
    var connectedDevice: BluetoothDevice? = null
    var onDeviceConnectedCallback: ((BluetoothDev) -> Unit)? = null

    private var onDescriptorRead: ((ByteArray) -> Unit)? = null
    private var onCharacteristicRead: ((UUID, ByteArray) -> Unit)? = null
    var onCharacteristicChangedCallback: ((ByteArray) -> Unit)? = null

    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null

    private var deviceType: BluetoothDev? = null

    companion object {
        private const val TAG = "BluetoothHandler"
    }

    enum class ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTING,
        DISCONNECTED,
        UNKNOWN;

        fun isActive() = (this == CONNECTING || this == CONNECTED)

        fun toTitle() = this.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    //endregion

    //region Values
    //val connectMessage = MutableStateFlow(ConnectionState.DISCONNECTED)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager: BluetoothManager? =
            activity.getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    private val bluetoothPermissionLauncher =
        activityResultRegistry.register(
            "bluetooth_permission_request",
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                btPermission = true
                if (bluetoothAdapter?.isEnabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btActivityResultLauncher.launch(enableBtIntent)
                } else {
                    onScanResult()
                }
            } else {
                btPermission = false
            }
        }

    private val btActivityResultLauncher =
        activityResultRegistry.register(
            "bt_activity_result",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onScanResult()
            }
        }
    //endregion

    //region Connect & permission functions
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkAndRequestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    fun bluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun connectToGattServer(device: BluetoothDevice): Boolean {
        if (connectedDevice != null) {
            Log.e(TAG, "Already connected to a device: ${connectedDevice?.name}. Disconnect first.")
            return false // Zwróć false, jeśli nie chcemy połączyć się ponownie
        }

//        if (hasBluetoothPermission()) {
//            checkAndRequestBluetoothPermission()
//            return false
//        }

        Log.i(TAG, "Attempting to connect to GATT server for device: ${device.name}")
        if (hasBluetoothPermission()) {
            bluetoothAdapter.let { adapter ->
                return try {
                    bluetoothGatt = device.connectGatt(activity, false, bluetoothGattCallback)
                    Log.e(TAG, "Status: ${bluetoothGatt != null}")
                    return bluetoothGatt != null //bluetoothGatt?.connect() == true

                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: ${e.message}")
                    false
                }
            }
        } else {
            checkAndRequestBluetoothPermission()
            return false
        }
    }

    fun setOnServicesDiscoveredCallback(callback: (List<BluetoothGattService>) -> Unit) {
        onServicesDiscovered = callback
    }
    //endregion

    fun setOnCharacteristicReadCallback(callback: (UUID, ByteArray) -> Unit) {
        onCharacteristicRead = callback
    }

    //region Get functions
    fun getBondedDevices(): Set<BluetoothDevice>? {
        checkAndRequestBluetoothPermission()
        return bluetoothAdapter?.bondedDevices
    }

    fun getConnectedDeviceName(): String? {
        if (!hasBluetoothPermission()) {
            checkAndRequestBluetoothPermission()
            Log.e("ConnectionManager", "Bluetooth permissions not granted")
        }
        return connectedDevice?.name // Zwracamy nazwę urządzenia, jeśli jest połączone
    }

    fun getServices(): List<BluetoothGattService> {
        return bluetoothGatt?.services ?: emptyList()
    }
    //endregion

    //region Enable indication functions (write descriptor, enable indication/notifications)
    suspend fun handleDeviceActions(services: List<BluetoothGattService>, deviceType: BluetoothDev?) {
        updateDateTime(services)
        when (deviceType) {
            is Thermometer -> {
                val characteristic = services
                    .find { it.uuid == BluetoothUUIDs.UUID_THERMOMETER_SERVICE }
                    ?.getCharacteristic(BluetoothUUIDs.UUID_THERMOMETER_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it) // Włącz powiadomienia dla termometru
                }
            }

            is WeightScale -> {
                val characteristic = services
                    .find { it.uuid == BluetoothUUIDs.UUID_WEIGHT_SCALE_SERVICE }
                    ?.getCharacteristic(BluetoothUUIDs.UUID_WEIGHT_SCALE_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it) // Włącz powiadomienia dla wagi
                }
            }

            is BloodPressureMonitor -> {
                val characteristic = services
                    .find { it.uuid == BluetoothUUIDs.UUID_BPM_SERVICE }
                    ?.getCharacteristic(BluetoothUUIDs.UUID_BPM_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it) // Włącz powiadomienia dla ciśnieniomierza
                }
            }

            else -> Log.e(TAG, "Unknown or unsupported device type: $deviceType")
        }
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
        properties and property != 0

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {

        if (!hasBluetoothPermission()) {
            checkAndRequestBluetoothPermission()
            Log.e("ConnectionManager", "Bluetooth permissions not granted")
            return
        }

        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e(
                    "ConnectionManager",
                    "${characteristic.uuid} doesn't support notifications/indications"
                )
                return
            }
        }

        characteristic.getDescriptor(BluetoothUUIDs.CCC_DESCRIPTOR_UUID)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e(
                    "ConnectionManager",
                    "setCharacteristicNotification failed for ${characteristic.uuid}"
                )
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e(
            "ConnectionManager",
            "${characteristic.uuid} doesn't contain the CCC descriptor!"
        )
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray) {
        bluetoothGatt?.let { gatt ->
            if (hasBluetoothPermission()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // API 33 (Tiramisu) lub wyższe
                        gatt.writeDescriptor(descriptor, value)
                    } else {
                        // Dla starszych wersji Androida
                        descriptor.value = value
                        gatt.writeDescriptor(descriptor)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: ${e.message}")
                }
            } else {
                checkAndRequestBluetoothPermission()
            }
        }
    }

    private var writeCharacteristicContinuation: Continuation<Unit>? = null

    suspend fun writeCharacteristic(
        services: List<BluetoothGattService>,
        servUUID: UUID,
        charUUID: UUID,
        value: ByteArray
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val characteristic = services.find { it.uuid == servUUID }?.getCharacteristic(charUUID)
        characteristic?.let {
            bluetoothGatt?.let { gatt ->
                if (hasBluetoothPermission()) {
                    try {
                        writeCharacteristicContinuation = continuation
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // API 33 (Tiramisu) lub wyższe
                            gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            // Dla starszych wersji Androida
                            characteristic.value = value
                            gatt.writeCharacteristic(characteristic)
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                        continuation.resumeWithException(e)
                    }
                } else {
                    checkAndRequestBluetoothPermission()
                    continuation.resumeWithException(SecurityException("Bluetooth permissions not granted"))
                }
            } ?: continuation.resumeWithException(IllegalStateException("BluetoothGatt not initialized"))
        } ?: continuation.resumeWithException(IllegalArgumentException("Characteristic not found"))
    }

    suspend fun updateDateTime(services: List<BluetoothGattService>) {
        val date = LocalDateTime.now()
        val byteArray = convertTimestampToByteArray(date)
        Log.e(TAG, "Saving a timestamp to a device: $date = $byteArray")

        when {
            services.any { it.uuid == BluetoothUUIDs.UUID_THERMOMETER_SERVICE } -> writeCharacteristic(services, BluetoothUUIDs.UUID_THERMOMETER_SERVICE, BluetoothUUIDs.UUID_DATETIME_CHARACTERISTIC, byteArray)
            services.any { it.uuid == BluetoothUUIDs.UUID_WEIGHT_SCALE_SERVICE } -> writeCharacteristic(services, BluetoothUUIDs.UUID_WEIGHT_SCALE_SERVICE, BluetoothUUIDs.UUID_DATETIME_CHARACTERISTIC, byteArray)
            services.any { it.uuid == BluetoothUUIDs.UUID_BPM_SERVICE } -> writeCharacteristic(services, BluetoothUUIDs.UUID_BPM_SERVICE, BluetoothUUIDs.UUID_DATETIME_CHARACTERISTIC, byteArray)
            else -> null
        }
    }
    //endregion

    //region Handle device type function
    private fun handleDeviceConnection(gatt: BluetoothGatt) {
        val services = gatt.services

        deviceType = when {
            services.any { it.uuid == BluetoothUUIDs.UUID_THERMOMETER_SERVICE } -> Thermometer()
            services.any { it.uuid == BluetoothUUIDs.UUID_WEIGHT_SCALE_SERVICE } -> WeightScale()
            services.any { it.uuid == BluetoothUUIDs.UUID_BPM_SERVICE } -> BloodPressureMonitor()
            else -> null
        }

        // Zgłoś typ urządzenia poprzez callback
        deviceType?.let {
            onDeviceConnectedCallback?.invoke(it)
            Log.e("Bluetooth", "Callback invoked with: $it , ${it.name}")
        } ?: run {
            Log.e("Bluetooth", "Unknown device type, callback not invoked")
            //onDeviceConnectedCallback?.invoke(null)
        }
    }
    //endregion

    //region Read characteristic by UUID function
    fun readCharacteristicByUUID(serviceUUID: UUID, characteristicUUID: UUID) {
        // Upewnij się, że bluetoothGatt jest połączony
        if (!hasBluetoothPermission()) {
            checkAndRequestBluetoothPermission()
            Log.e("ConnectionManager", "Bluetooth permissions not granted")
            return
        }

        if (bluetoothGatt != null) {
            // Znajdź usługę
            val service = bluetoothGatt?.getService(serviceUUID)
            if (service != null) {
                // Znajdź charakterystykę
                val characteristic = service.getCharacteristic(characteristicUUID)
                if (characteristic != null) {
                    // Odczytaj charakterystykę
                    bluetoothGatt?.readCharacteristic(characteristic)
                } else {
                    Log.e(TAG, "Characteristic not found for UUID: $characteristicUUID")
                }
            } else {
                Log.e(TAG, "Service not found for UUID: $serviceUUID")
            }
        } else {
            Log.e(TAG, "BluetoothGatt is null. Device may not be connected.")
        }
    }
    //endregion

    //region Read all needed characteristics
    suspend fun readCharacteristicValue(serviceUUID: UUID, characteristicUUID: UUID): String? {
        return suspendCoroutine { continuation ->
            setOnCharacteristicReadCallback { uuid, value ->
                if (uuid == characteristicUUID) {
                    continuation.resume(value.toHexString())
                }
            }
            readCharacteristicByUUID(serviceUUID, characteristicUUID)
        }
    }

    suspend fun readAllCharacteristics(
        services: List<BluetoothGattService>,
        serviceAndCharacteristicUUIDs: List<Pair<UUID, UUID>>
    ): Map<UUID, String> {
        val characteristicValues = mutableMapOf<UUID, String>()
        for ((serviceUUID, characteristicUUID) in serviceAndCharacteristicUUIDs) {
            // Znajdź usługę na podstawie UUID
            val service = services.find { it.uuid == serviceUUID }
            if (service != null) {
                // Znajdź charakterystykę na podstawie UUID
                val characteristic = service.getCharacteristic(characteristicUUID)
                if (characteristic != null) {
                    Log.d("Bluetooth", "Reading Characteristic UUID: ${characteristic.uuid}")
                    val value = readCharacteristicValue(service.uuid, characteristic.uuid)
                    value?.let {
                        Log.d("Bluetooth", "Characteristic UUID: ${characteristic.uuid} Value: $it")
                        characteristicValues[characteristic.uuid] = it
                    } ?: Log.e("Bluetooth", "Failed to read characteristic: ${characteristic.uuid}")
                } else {
                    Log.e(TAG, "Characteristic $characteristicUUID not found in service $serviceUUID")
                }
            } else {
                Log.e(TAG, "Service $serviceUUID not found.")
            }
        }
        return characteristicValues
    }
    //endregion

//    fun BluetoothDevice.isAlreadyConnected(): Boolean {
//        return try {
//            javaClass.getMethod("isConnected").invoke(this) as? Boolean? ?: false
//
//        } catch (e: Throwable) {
//            false
//        }
//    }

    // Odbiornik Bluetooth, nasłuchujący zdarzeń związanych z połączeniem i rozłączeniem
//    private val bluetoothReceiver = object : BroadcastReceiver() {
//        @SuppressLint("MissingPermission")
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//
//            when (action) {
//                BluetoothDevice.ACTION_ACL_CONNECTED -> {
//                    Log.e("BluetoothReceiver", "Device connected: ${device?.name}")
//                }
//                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
//                    Log.e("BluetoothReceiver", "Disconnect requested for device: ${device?.name}")
//                }
//                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
//                    Log.e("BluetoothReceiver", "Device disconnected: ${device?.name}")
//                    retryConnection()  // Wywołaj metodę ponownego połączenia
//                }
//            }
//        }
//    }
//
//    // Funkcja rejestrująca odbiornik Bluetooth
//    fun registerReceiver() {
//        val filter = IntentFilter().apply {
//            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
//            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
//            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
//        }
//        activity.registerReceiver(bluetoothReceiver, filter)  // Używamy kontekstu aktywności
//    }
//
//    // Funkcja usuwająca odbiornik Bluetooth
//    fun unregisterReceiver() {
//        activity.unregisterReceiver(bluetoothReceiver)  // Usuwamy odbiornik, gdy aktywność zostaje zniszczona
//    }
//
//    // Funkcja do próby ponownego połączenia
//    private fun retryConnection() {
//        // Tutaj możesz dodać logikę próby ponownego połączenia z urządzeniem
//        Log.e("BluetoothHandler", "Retrying Bluetooth connection...")
//        // Możesz ponownie wywołać metodę connectToGattServer() na kolejnym urządzeniu, np.:
//        // connectToGattServer()
//    }



    private val bluetoothGattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//            super.onConnectionStateChange(gatt, status, newState)
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Log.i(TAG, "Connected to GATT server.")
//                connectedDevice = gatt.device
//                if (hasBluetoothPermission()) {
//                    try {
//                        gatt.discoverServices()
//                    } catch (e: SecurityException) {
//                        Log.e(TAG, "SecurityException: ${e.message}")
//                    }
//                }
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.i(TAG, "Disconnected from GATT server.")
//                if (hasBluetoothPermission()) {
//                    try {
//                        gatt.close()
//                        bluetoothGatt = null
//                    } catch (e: SecurityException) {
//                        Log.e(TAG, "SecurityException: ${e.message}")
//                    }
//                }
//
//            }
//        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.e(TAG, "Connecting to device...")
                    onConnectionStateChanged?.let { it(ConnectionState.CONNECTING) }
                }

                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server: ${gatt.device.name}")
                    onConnectionStateChanged?.let {
                        Log.e(TAG, "Emitting ConnectionState.CONNECTED for ${gatt.device.name}")
                        it(ConnectionState.CONNECTED)
                    }
                    connectedDevice = gatt.device
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                    }
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    Log.e(TAG, "Disconnecting from device...")
                    onConnectionStateChanged?.let { it(ConnectionState.DISCONNECTING) }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server: ${gatt.device.name}")
                    bluetoothGatt?.close()
                    onConnectionStateChanged?.let {
                        Log.e(TAG, "Emitting ConnectionState.DISCONNECTED for ${gatt.device.name}")
                        it(ConnectionState.DISCONNECTED)
                    }
                    connectedDevice = null
                    try {
                        //gatt.disconnect()
                        //gatt.close()
                        //bluetoothGatt = null
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                    }
                }


            }

            // Obsługa błędów
//            if (status == BluetoothGatt.GATT_FAILURE || status == 133) {
//                Log.e(TAG, "Connection failed with status: $status")
//                bluetoothGatt?.close()
//                onConnectionStateChanged?.let { it(ConnectionState.DISCONNECTED) }
//                if (hasBluetoothPermission()) {
//                    try {
//                        //bluetoothGatt?.close()
//                        //bluetoothGatt = null
//                    } catch (e: SecurityException) {
//                        Log.e(TAG, "SecurityException: ${e.message}")
//                    }
//                }
                // Możesz tu dodać dodatkową logikę, np. ponowną próbę połączenia
//                Handler(Looper.getMainLooper()).postDelayed({
                    // Możesz tutaj spróbować ponownie nawiązać połączenie
//                }, 1000)
//            }
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.")
                onServicesDiscovered?.invoke(gatt.services) //???

                //updateDateTime(gatt.services)
                handleDeviceConnection(gatt)

            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        //region Deprecated (?) read functions
        @Deprecated("Deprecated")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(gatt, characteristic, characteristic.value)
            }
        }
        //endregion

        // API 33+ overrides

        //region Read functions
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(gatt, characteristic, value)
            }
        }

        private fun handleCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(TAG, "Characteristic ${characteristic.uuid} read: ${value.contentToString()}")
            // Process the read data
            onCharacteristicRead?.invoke(characteristic.uuid, value)
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleDescriptorRead(gatt, descriptor, descriptor.value)
            }
        }

        private fun handleDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            value: ByteArray
        ) {
            Log.i(TAG, "Descriptor read: ${value.contentToString()}")
                // Process the read data
            onDescriptorRead?.invoke(value)
        }
        //endregion

        //region Write functions
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor write successful: ${descriptor.uuid}")
            } else {
                Log.e(TAG, "Descriptor write failed: ${descriptor.uuid}, status: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successful")
                // Notify the coroutine that the write has completed
                writeCharacteristicContinuation?.resume(Unit)
            } else {
                Log.e(TAG, "Characteristic write failed with status: $status")
                writeCharacteristicContinuation?.resumeWithException(Exception("Write failed with status: $status"))
            }
        }

//        override fun onCharacteristicWrite(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            super.onCharacteristicWrite(gatt, characteristic, status)
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.i(TAG, "Characteristic write successful: ${characteristic.uuid}")
//            } else {
//                Log.e(TAG, "Characteristic write failed: ${characteristic.uuid}, status: $status")
//            }
//        }
        //endregion

        //region "Subscribe" to characteristic value functions
        @Deprecated("Deprecated for Android 13+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value}")
                onCharacteristicChangedCallback?.invoke(value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: $value")
            }
        }


        //endregion
    }
}

