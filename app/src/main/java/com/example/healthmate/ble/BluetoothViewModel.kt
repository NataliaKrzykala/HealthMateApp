package com.example.healthmate.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothViewModel(
bluetoothHandler: BluetoothHandler
) : ViewModel() {

    private val _currentDevice = MutableStateFlow<BluetoothDev?>(null)
    val currentDevice: StateFlow<BluetoothDev?> = _currentDevice.asStateFlow()

    private val _characteristicValue = MutableStateFlow<ByteArray?>(null)
    val characteristicValue: StateFlow<ByteArray?> = _characteristicValue.asStateFlow()

    // Map z wartościami charakterystyk
    private val _characteristicValues = MutableStateFlow<Map<UUID, String>>(emptyMap())
    val characteristicValues: StateFlow<Map<UUID, String>> = _characteristicValues

    // Funkcja do aktualizacji wartości charakterystyk
    fun updateCharacteristicValues(values: Map<UUID, String>) {
        _characteristicValues.value = values
    }
    fun setCurrentDevice(device: BluetoothDev) {
        _currentDevice.value = device
    }
    fun updateCharacteristicValue(value: ByteArray) {
        _characteristicValue.value = value
    }
}