package com.example.healthmate.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothViewModel(
bluetoothHandler: BluetoothHandler
) : ViewModel() {

    private val _currentDevice = MutableStateFlow<BluetoothDev?>(null)
    val currentDevice: StateFlow<BluetoothDev?> = _currentDevice.asStateFlow()

    private val _characteristicValue = MutableStateFlow<ByteArray?>(null)
    val characteristicValue: StateFlow<ByteArray?> = _characteristicValue.asStateFlow()

    fun setCurrentDevice(device: BluetoothDev) {
        _currentDevice.value = device
    }
    fun updateCharacteristicValue(value: ByteArray) {
        _characteristicValue.value = value
    }
}