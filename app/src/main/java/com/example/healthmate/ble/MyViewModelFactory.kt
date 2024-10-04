package com.example.healthmate.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BluetoothViewModelFactory(private val bluetoothHandler: BluetoothHandler) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            return BluetoothViewModel(bluetoothHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
