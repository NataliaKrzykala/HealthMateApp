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
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import com.example.healthmate.data.convertTimestampToByteArray
import kotlin.coroutines.Continuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.provider.Settings
import android.app.AlertDialog
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.healthmate.R


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
    }
    //endregion

    //region Values
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager: BluetoothManager? =
            activity.getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    private val bluetoothPermissionLauncher =
        activityResultRegistry.register(
            "bluetooth_permission_request",
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isBluetoothScanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] == true
            val isBluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            val isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val isLocationCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (isBluetoothScanGranted && isBluetoothConnectGranted && isLocationGranted) {
                btPermission = true
                // Po przyznaniu uprawnień, jeśli Bluetooth nie jest włączony, włącz go
                if (bluetoothAdapter?.isEnabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btActivityResultLauncher.launch(enableBtIntent)
                }
//                else {
//                    onScanResult() // Kontynuuj operację skanowania
//                }
//                var isLocationDialogShown = false
//                if (!isLocationEnabled()) {
//                    Log.e("PERMISSIONS", "ELO 4")
//                    showLocationRequestDialog{isLocationDialogShown = false}
//                    //requestEnableLocation()
//                } else {
//                    //continueBluetoothOperations()
//                }

            } else {
                btPermission = false
                // Obsłuż przypadek, gdy uprawnienia są odrzucone
            }
        }

    private val btActivityResultLauncher =
        activityResultRegistry.register(
            "bt_activity_result",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onScanResult()
            } else {
                handleBluetoothRefused()
            }
        }

//    fun showLocationEnablePrompt() {
//        // Wyświetl dedykowany ekran lub dialog w Jetpack Compose
//        activity.runOnUiThread {
//            Toast.makeText(activity, "Włącz lokalizację, aby kontynuować", Toast.LENGTH_SHORT).show()
//        }
//    }

    fun handleBluetoothRefused() {
        Toast.makeText(activity, activity.getString(R.string.turn_on_bt), Toast.LENGTH_LONG).show()
    }


    fun showLocationRequestDialog(onDialogClosed: () -> Unit) {
        val builder = AlertDialog.Builder(activity)

        // Ustawienie niestandardowego layoutu dla dialogu
        //val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_custom_layout, null)

        // Ustawienie tła i zaokrąglenia
        val dialog = builder
            .setTitle(activity.getString(R.string.location_request_title))
            .setMessage(activity.getString(R.string.location_request_message))
            //.setView(dialogView) // Wstawiamy niestandardowy layout
            .setPositiveButton(activity.getString(R.string.positive_button_text)) { _, _ ->
                requestEnableLocation()
                onDialogClosed() // Zresetuj flagę po akceptacji
            }
            .setNegativeButton(activity.getString(R.string.negative_button_text)) { dialog, _ ->
                dialog.dismiss()
                onDialogClosed() // Zresetuj flagę po odmowie
            }
            .setCancelable(false)
            .create()

        // Zmieniamy kolory przycisków po tym, jak dialog się wyświetli
        dialog.setOnShowListener {
            // Zmiana kolorów przycisków po pokazaniu dialogu
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary)) // Kolor przycisku "Tak"
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary)) // Kolor przycisku "Nie"
        }

        // Zmiana tła dialogu na zaokrąglony i kolorowy
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(activity, R.drawable.dialog_custom_layout)
        )

        dialog.show()
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager =
            activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestEnableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        locationActivityResultLauncher.launch(intent)
    }

    private val locationActivityResultLauncher =
        activityResultRegistry.register(
            "location_activity_result",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (isLocationEnabled()) {
                //continueBluetoothOperations()
            } else {
                // Obsłuż przypadek, gdy lokalizacja nie została włączona
                Log.e(TAG, "Lokalizacja nie jest włączona.")
            }
        }
    //endregion

    //region Connect & permission functions

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun hasBluetoothPermission(): Boolean {
        Log.e("PERMISSIONS", "HAS?")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e("PERMISSIONS", "HAS1")
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED //&&
                    //activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    //activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            Log.e("PERMISSIONS", "HAS2")
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkAndRequestBluetoothPermission() {
        Log.e("PERMISSIONS", "CHECK?")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e("PERMISSIONS", "CHECK1")
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            Log.e("PERMISSIONS", "CHECK2")
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

            )
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isEnabled = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if(!isEnabled){
            Toast.makeText(activity, "Wymagane połączenie z siecią", Toast.LENGTH_SHORT).show()
        }
        return isEnabled
    }
    //endregion

//    private val bluetoothPermissionLauncher =
//        activityResultRegistry.register(
//            "bluetooth_permission_request",
//            ActivityResultContracts.RequestPermission()
//        ) { isGranted: Boolean ->
//            if (isGranted) {
//                btPermission = true
//                if (bluetoothAdapter?.isEnabled == false) {
//                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                    btActivityResultLauncher.launch(enableBtIntent)
//                } else {
//                    onScanResult()
//                }
//            } else {
//                btPermission = false
//            }
//        }
//
//    private val btActivityResultLauncher =
//        activityResultRegistry.register(
//            "bt_activity_result",
//            ActivityResultContracts.StartActivityForResult()
//        ) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                onScanResult()
//            }
//        }
//
//
//    //endregion
//
//    //region Connect & permission functions
//    fun hasBluetoothPermission(): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
//            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
//        } else {
//            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//
//    fun checkAndRequestBluetoothPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            bluetoothPermissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.BLUETOOTH_CONNECT
//                ).toString()
//            )
//        } else {
//            bluetoothPermissionLauncher.launch(
//                arrayOf(Manifest.permission.BLUETOOTH_ADMIN).toString()
//            )
//        }
//    }

    fun connectToGattServer(device: BluetoothDevice): Boolean {
        if (connectedDevice != null) {
            Log.e(TAG, "Already connected to a device: ${connectedDevice?.name}. Disconnect first.")
            return false // Zwróć false, jeśli nie chcemy połączyć się ponownie
        }

        Log.i(TAG, "Attempting to connect to GATT server for device: ${device.name}")
        if (hasBluetoothPermission()) {
            bluetoothAdapter.let { adapter ->
                return try {
                    bluetoothGatt = device.connectGatt(activity, false, bluetoothGattCallback)
                    Log.e(TAG, "Status: ${bluetoothGatt != null} for ${device.name}")
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

//        val characteristic = services
//            .find { it.uuid == BluetoothUUIDs.UUID_GAP_SERVICE }
//            ?.getCharacteristic(BluetoothUUIDs.UUID_PREFERRED_PARAMETERS_CHARACTERISTIC)
//        characteristic?.let {
//            enableNotifications(it)
//        }

        //updateDateTime(services)

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
            Log.e("Bluetooth", "Callback invoked with: $it , ${it.name}")
            onDeviceConnectedCallback?.invoke(it)
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

    //region Scanning devices
//    private val processedDevices = mutableSetOf<String>()
    private var isScanning = false

    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.e("BluetoothHandler", "Device found: ${result.device.name} (${result.device.address})")
            if (!hasBluetoothPermission()) {
                checkAndRequestBluetoothPermission()
                Log.e("ConnectionManager", "Bluetooth permissions not granted")
                return
            }
            // Ignorujemy urządzenia, które już przetworzyliśmy
            val deviceAddress = result.device.address
//            if (processedDevices.contains(deviceAddress)) {
//                Log.e("BluetoothHandler", "Device already processed: $deviceAddress")
//                return
//            }

            // Dodajemy urządzenie do listy przetworzonych
            //processedDevices.add(deviceAddress)

            // Sprawdzamy, czy urządzenie spełnia nasze kryteria
            val deviceName = result.device.name
            if (deviceName?.contains("A&D") == true || deviceName?.contains("nRF") == true) {
                Log.e("BluetoothHandler", "Matching device found: $deviceName ($deviceAddress)")

                // Zatrzymujemy skanowanie
                stopScanning()

                // Próba połączenia z urządzeniem
                connectToGattServer(result.device)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            if (!hasBluetoothPermission()) {
                checkAndRequestBluetoothPermission()
                Log.e("ConnectionManager", "Bluetooth permissions not granted")
                return
            }
            Log.e("BluetoothHandler", "Batch result")
            super.onBatchScanResults(results)
            //scanResults.addAll(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BluetoothHandler", "Scan failed with error: $errorCode")
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> Log.e("BluetoothHandler", "Scan already started")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.e("BluetoothHandler", "Application registration failed")
                SCAN_FAILED_INTERNAL_ERROR -> Log.e("BluetoothHandler", "Internal error")
                SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.e("BluetoothHandler", "Feature unsupported")
                else -> Log.e("BluetoothHandler", "Unknown scan failure")
            }

            stopScanning()

        }
    }

    fun startScanning() {
        if (isScanning) {
            Log.e("BluetoothHandler", "Scanning already in progress.")
            return
        }

        if (!hasBluetoothPermission()) {
            checkAndRequestBluetoothPermission()
            Log.e("ConnectionManager", "Bluetooth permissions not granted")
            return
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        val scanFilters = listOf<ScanFilter>() // Brak filtrów, aby znaleźć wszystkie urządzenia

        bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
        isScanning = true
        Log.e("BluetoothHandler", "Started scanning with filters.")
    }

    fun stopScanning() {
        if (!isScanning) {
            Log.e("BluetoothHandler", "Scanning not in progress.")
            return
        }

        if (!hasBluetoothPermission()) {
            checkAndRequestBluetoothPermission()
            Log.e("ConnectionManager", "Bluetooth permissions not granted")
            return
        }

        bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.e("BluetoothHandler", "Stopped scanning")
    }
    //endregion

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.e(TAG, "Connecting to device...")
                    onConnectionStateChanged?.invoke(ConnectionState.CONNECTING)
                }

                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server: ${gatt.device.name}")
                    Log.e(TAG, "Emitting ConnectionState.CONNECTED for ${gatt.device.name}")
                    onConnectionStateChanged?.invoke(ConnectionState.CONNECTED)
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
                    Log.e(TAG, "Emitting ConnectionState.DISCONNECTED for ${gatt.device.name}")
                    onConnectionStateChanged?.invoke(ConnectionState.DISCONNECTED)
                    bluetoothGatt?.close()
                    connectedDevice = null
                }


            }
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

