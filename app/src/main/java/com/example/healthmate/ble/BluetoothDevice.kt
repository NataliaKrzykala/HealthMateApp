package com.example.healthmate.ble

import com.example.healthmate.ui.parseTemperatureFromByte
import com.example.healthmate.ui.parseTimestampFromByte
import java.util.UUID

abstract class BluetoothDev(val name: String) {
    abstract fun parseData(characteristicValue: ByteArray?): Map<String, Any>
    abstract fun getDisplayData(): List<String>
}

class Thermometer : BluetoothDev("Thermometer") {
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        val temp = parseTemperatureFromByte(characteristicValue?.copyOfRange(1, 5))
        val timestamp = parseTimestampFromByte(characteristicValue?.copyOfRange(5, 12))
        val resultMap = mutableMapOf<String, Any>()

        temp?.let { resultMap["Temperature"] = it }
        timestamp?.let { resultMap["Timestamp"] = it.time }

        return resultMap
    }

    override fun getDisplayData(): List<String> {
        return listOf("Temperature", "Timestamp")
    }
}

class BloodPressureMonitor : BluetoothDev("Blood Pressure Monitor") {
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        val temp = parseTemperatureFromByte(characteristicValue?.copyOfRange(1, 5))
        val timestamp = parseTimestampFromByte(characteristicValue?.copyOfRange(5, 12))
        val resultMap = mutableMapOf<String, Any>()

        temp?.let { resultMap["Temperature"] = it } // Dodajemy tylko jeśli nie jest null
        timestamp?.let { resultMap["Timestamp"] = it.time } // Dodajemy tylko jeśli nie jest null

        return resultMap
    }

    override fun getDisplayData(): List<String> {
        return listOf("Systolic", "Diastolic")
    }
}
