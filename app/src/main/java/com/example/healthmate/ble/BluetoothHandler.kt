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

class BluetoothHandler(
    private val activity: Activity,
    private val activityResultRegistry: ActivityResultRegistry,
    private val onScanResult: () -> Unit,
) {

    private var btPermission = false
    private var bluetoothGatt: BluetoothGatt? = null

    private var onServicesDiscovered: ((List<BluetoothGattService>) -> Unit)? = null
    private var onCharacteristicRead: ((ByteArray) -> Unit)? = null

    companion object {
        private const val TAG = "BluetoothHandler"
    }

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
                onServicesDiscovered?.invoke(gatt.services)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicRead(gatt, characteristic, characteristic.value)
            }
        }

        @Deprecated("Deprecated")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleCharacteristicChanged(gatt, characteristic, characteristic.value)
        }

        // API 33+ overrides
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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChanged(gatt, characteristic, value)
        }

        // Handle characteristic read and changed with a unified method
        private fun handleCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(TAG, "Characteristic read: ${value.contentToString()}")
            // Process the read data
            onCharacteristicRead?.invoke(value)
        }

        private fun handleCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(TAG, "Characteristic changed: ${value.contentToString()}")
            // Process the changed data
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
        }
    }
}

