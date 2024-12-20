package com.example.healthmate.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BleObserver(
    private val activity: ComponentActivity,
    //private val bleManager: BluetoothManager, // Dodaj bleManager jako zależność
    private val bluetoothHandler: BluetoothHandler
) : DefaultLifecycleObserver {

    val btAdapter: BluetoothAdapter? by lazy {
        val bleManager: BluetoothManager? =
            activity.getSystemService(BluetoothManager::class.java)
        bleManager?.adapter
    }

    private val registry: ActivityResultRegistry = activity.activityResultRegistry
    lateinit var btEnableResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        createBroadcastReceiver()
        btEnableResultLauncher = registerHandler(owner, "EnableBLE")

        ContextCompat.registerReceiver(
            activity,
            broadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun createBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action

                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    if (btAdapter?.state == BluetoothAdapter.STATE_OFF) {
                        bluetoothHandler.stopScanning()
                        val btEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        btEnableResultLauncher.launch(btEnableIntent)
                        return
                    }
                    if (btAdapter?.state == BluetoothAdapter.STATE_ON) {
                        bluetoothHandler.startScanning()
                        return
                    }
                }
            }
        }
    }

    private fun registerHandler(owner: LifecycleOwner, key: String) = registry.register(
        key,
        owner,
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bluetoothHandler.startScanning()
        }
    }
}