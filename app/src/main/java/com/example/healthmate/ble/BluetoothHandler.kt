package com.example.healthmate.ble

import android.Manifest
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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BluetoothHandler(
    private val activity: Activity,
    private val activityResultRegistry: ActivityResultRegistry,
    private val onScanResult: () -> Unit
) {

    //region Variables
    private var btPermission = false
    private var bluetoothGatt: BluetoothGatt? = null

    private var onServicesDiscovered: ((List<BluetoothGattService>) -> Unit)? = null
    var connectedDevice: BluetoothDevice? = null
    var onDeviceConnectedCallback: ((BluetoothDev) -> Unit)? = null

    //var onDeviceTypeDetermined: ((BluetoothDev?) -> Unit)? = null

    private var onDescriptorRead: ((ByteArray) -> Unit)? = null
    private var onCharacteristicRead: ((UUID, ByteArray) -> Unit)? = null
    var onCharacteristicChangedCallback: ((ByteArray) -> Unit)? = null

    companion object {
        private const val TAG = "BluetoothHandler"
    }
    //endregion

    //region Values
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager: BluetoothManager? = activity.getSystemService(BluetoothManager::class.java)
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

    fun connectToGattServer(device: BluetoothDevice) {
        if (hasBluetoothPermission()) {
            bluetoothAdapter.let { adapter ->
                try {
                    bluetoothGatt = device.connectGatt(activity, false, bluetoothGattCallback)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: ${e.message}")
                }
            }
        } else {
            checkAndRequestBluetoothPermission()
        }
    }

    fun setOnServicesDiscoveredCallback(callback: (List<BluetoothGattService>) -> Unit) {
        onServicesDiscovered = callback
    }
    //endregion

    //region Read characteristic & descriptor functions - not used currently
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            if (hasBluetoothPermission()) {
                try {
                    gatt.readCharacteristic(characteristic)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: ${e.message}")
                    // Obsługa braku uprawnień, np. wyświetlenie komunikatu do użytkownika
                }
            } else {
                checkAndRequestBluetoothPermission()
            }
        }
    }

    fun setOnCharacteristicReadCallback(callback: (UUID, ByteArray) -> Unit) {
        onCharacteristicRead = callback
    }

    fun readDescriptor(descriptor: BluetoothGattDescriptor) {
        bluetoothGatt?.let { gatt ->
            if (hasBluetoothPermission()) {
                try {
                    gatt.readDescriptor(descriptor)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: ${e.message}")
                    // Obsługa braku uprawnień, np. wyświetlenie komunikatu do użytkownika
                }
            } else {
                checkAndRequestBluetoothPermission()
            }
        }
    }

    fun setOnDescriptorReadCallback(callback: (ByteArray) -> Unit) {
        onDescriptorRead = callback
    }
    //endregion

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
    //endregion

    //region Enable indication functions (write descriptor, enable indication/notifications)
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
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(BluetoothUUIDs.CCC_DESCRIPTOR_UUID)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }
    //endregion

    //region Handle device type function
    private fun handleDeviceConnection(gatt: BluetoothGatt) {
        val services = gatt.services
        val deviceType = when {
            services.any { it.uuid == BluetoothUUIDs.UUID_THERMOMETER_SERVICE } -> Thermometer()
            services.any { it.uuid == BluetoothUUIDs.UUID_BPM_SERVICE } -> BloodPressureMonitor()
            else -> null
        }

        // Zgłoś typ urządzenia poprzez callback
        deviceType?.let {
            onDeviceConnectedCallback?.invoke(it)
        } ?: run {
            Log.e(TAG, "Unknown device type")
            //onDeviceConnectedCallback?.invoke(null)  // Przekaż null, jeśli nieznany typ
        }
    }

    fun handleDeviceActions(services: List<BluetoothGattService>, deviceType: BluetoothDev?) {
        when (deviceType) {
            is Thermometer -> {
                val characteristic = services
                    .find { it.uuid == BluetoothUUIDs.UUID_THERMOMETER_SERVICE }
                    ?.getCharacteristic(BluetoothUUIDs.UUID_THERMOMETER_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it) // Włącz powiadomienia dla termometru
                }
            }
            is BloodPressureMonitor -> {
                val characteristic = services
                    .find { it.uuid == BluetoothUUIDs.UUID_BPM_SERVICE }
                    ?.getCharacteristic(BluetoothUUIDs.UUID_BPM_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it) // Włącz powiadomienia dla monitora ciśnienia
                }
            }
            else -> Log.e(TAG, "Unknown or unsupported device type")
        }
    }


    /*private fun handleDeviceConnection(gatt: BluetoothGatt) {
        val services = gatt.services
        val deviceType = when {
            services.any { it.uuid == BluetoothUUIDs.UUID_THERMOMETER_SERVICE } -> Thermometer()
            services.any { it.uuid == BluetoothUUIDs.UUID_BPM_SERVICE } -> BloodPressureMonitor()
            else -> null
        }

        deviceType?.let {
            onDeviceConnectedCallback?.invoke(it)
        } ?: run {
            Log.e(TAG, "Unknown device type")
        }

        when (deviceType) {
            is Thermometer -> {
                val characteristic = gatt.getService(BluetoothUUIDs.UUID_THERMOMETER_SERVICE)
                    ?.getCharacteristic(BluetoothUUIDs.UUID_THERMOMETER_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it)
                }
            }
            is BloodPressureMonitor -> {
                val characteristic = gatt.getService(BluetoothUUIDs.UUID_BPM_SERVICE)
                    ?.getCharacteristic(BluetoothUUIDs.UUID_BPM_CHARACTERISTIC)
                characteristic?.let {
                    enableNotifications(it) // Włącz powiadomienia
                }
            }
            else -> Log.e(TAG, "Unknown device type")
        }
    }*/
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

    @OptIn(ExperimentalStdlibApi::class)
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
        services: List<BluetoothGattService>
    ): Map<UUID, String> {
        val characteristicValues = mutableMapOf<UUID, String>()
        val excludedUuid = UUID.fromString("00002A1C-0000-1000-8000-00805F9B34FB") // UUID 2A1C

        for (service in services) {
            Log.d("Bluetooth", "Service UUID: ${service.uuid}")
            for (characteristic in service.characteristics) {
                if (characteristic.uuid == excludedUuid) {
                    Log.d("Bluetooth", "Skipping characteristic UUID: ${characteristic.uuid}")
                    continue // Pomijanie charakterystyki o UUID 2A1C
                }
                Log.d("Bluetooth", "Reading Characteristic UUID: ${characteristic.uuid}")
                if (hasBluetoothPermission()) {
                    val value = readCharacteristicValue(service.uuid, characteristic.uuid)
                    value?.let {
                        Log.d("Bluetooth", "Characteristic UUID: ${characteristic.uuid} Value: $it")
                        characteristicValues[characteristic.uuid] = it
                    } ?: Log.e("Bluetooth", "Failed to read characteristic: ${characteristic.uuid}")
                } else {
                    Log.e("Bluetooth", "Bluetooth permissions not granted.")
                }
            }
        }
        return characteristicValues
    }

    fun getServices(): List<BluetoothGattService> {
        return bluetoothGatt?.services ?: emptyList()
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                if (hasBluetoothPermission()) {
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: ${e.message}")
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered.")
                onServicesDiscovered?.invoke(gatt.services) //???

                handleDeviceConnection(gatt)

            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        //region Deprecated functions ? needed?
        @Deprecated("Deprecated")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(gatt, characteristic, characteristic.value)
            }
        }

        //onDescriptorRead?, onCharacteristicChanged?,
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
            Log.i(TAG, "Characteristic read: ${value.contentToString()}")
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

