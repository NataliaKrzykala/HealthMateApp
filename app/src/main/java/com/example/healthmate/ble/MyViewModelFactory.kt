package com.example.healthmate.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthmate.data.HealthMateRepository

class HealthMateViewModelFactory(
    private val bluetoothHandler: BluetoothHandler,
    private val repository: HealthMateRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothViewModel(bluetoothHandler, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
