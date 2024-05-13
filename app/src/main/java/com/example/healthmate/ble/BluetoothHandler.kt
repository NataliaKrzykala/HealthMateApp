package com.example.healthmate.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts

class BluetoothHandler(
    private val activity: Activity,
    private val activityResultRegistry: ActivityResultRegistry,
    private val onScanResult: () -> Unit
) {

    private var btPermission = false

    private val bluetoothPermissionLauncher =
        activityResultRegistry.register(
            "bluetooth_permission_request",
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                val bluetoothManager: BluetoothManager =
                    activity.getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
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
            bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    fun bluetoothEnabled(): Boolean{
        return bluetoothAdapter?.isEnabled == true
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager: BluetoothManager? = activity.getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    fun getBondedDevices(): Set<BluetoothDevice>? {
        return if (activity.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.bondedDevices
        } else {
            null
        }
    }


}
