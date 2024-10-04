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

class BluetoothHandler(
    private val activity: Activity,
    private val activityResultRegistry: ActivityResultRegistry,
    private val onScanResult: () -> Unit
) {

    private var btPermission = false
    private var bluetoothGatt: BluetoothGatt? = null

    private var onServicesDiscovered: ((List<BluetoothGattService>) -> Unit)? = null
    var onDeviceConnectedCallback: ((BluetoothDev) -> Unit)? = null
    private var onCharacteristicRead: ((ByteArray) -> Unit)? = null
    private var onDescriptorRead: ((ByteArray) -> Unit)? = null
    var onCharacteristicChangedCallback: ((ByteArray) -> Unit)? = null
    var connectedDevice: BluetoothDevice? = null


    companion object {
        private const val TAG = "BluetoothHandler"
    }


    //region vals
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

    //region connect & permission functions
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

    fun getBondedDevices(): Set<BluetoothDevice>? {
        checkAndRequestBluetoothPermission()
        return bluetoothAdapter?.bondedDevices
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
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

    //region read functions
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

    fun setOnCharacteristicReadCallback(callback: (ByteArray) -> Unit) {
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

    fun getConnectedDeviceName(): String? {
        if (!hasBluetoothPermission()) {
            checkAndRequestBluetoothPermission()
            Log.e("ConnectionManager", "Bluetooth permissions not granted")
        }
        return connectedDevice?.name // Zwracamy nazwę urządzenia, jeśli jest połączone
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

    private fun handleDeviceConnection(gatt: BluetoothGatt) {
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
                    enableNotifications(it) // Włącz powiadomienia dla ciśnieniomierza
                }
            }
            else -> Log.e(TAG, "Unknown device type")
        }
    }

    /*fun parseMultipleCharacteristics(uuidList: List<UUID>) {
        // Iterujemy przez każdą UUID
        uuidList.forEach { uuid ->
            // Szukamy charakterystyki we wszystkich dostępnych serwisach
            val characteristic = bluetoothGatt?.services?.flatMap { it.characteristics }?.find { it.uuid == uuid }

            // Odczytaj charakterystykę, jeśli istnieje
            characteristic?.let {
                readCharacteristic(it)
            } ?: Log.e(TAG, "Characteristic not found for UUID: $uuid")
        }
    }*/

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


    @OptIn(ExperimentalStdlibApi::class)
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

        //region deprecated functions ?
        @Deprecated("Deprecated")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(gatt, characteristic, characteristic.value)
            }
        }
        //endregion

        // API 33+ overrides

        //region read functions
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
            onCharacteristicRead?.invoke(value)
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

        @Deprecated("Deprecated for Android 13+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value.toHexString()}")
                onCharacteristicChangedCallback?.invoke(value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val newValueHex = value.toHexString()
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: $newValueHex")
            }
        }
    }
}

