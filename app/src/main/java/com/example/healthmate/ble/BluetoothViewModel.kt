package com.example.healthmate.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BluetoothViewModel(
    private val bluetoothHandler: BluetoothHandler
) : ViewModel() {
    private val _services = MutableLiveData<List<BluetoothGattService>>()
    val services: LiveData<List<BluetoothGattService>> = _services

    init {
        bluetoothHandler.setOnServicesDiscoveredCallback { discoveredServices ->
            _services.postValue(discoveredServices)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothHandler.connectToGattServer(device)
    }
}