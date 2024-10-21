package com.example.healthmate.ble

import android.util.Log
import com.example.healthmate.ui.parseMeasurePlaceFromByte
import com.example.healthmate.ui.parseSettingsFromByte
import com.example.healthmate.ui.parseTemperatureFromByte
import com.example.healthmate.ui.parseTimestampFromByte
import java.util.UUID

abstract class BluetoothDev(val name: String) {
    abstract fun parseData(characteristicValue: ByteArray?): Map<String, Any>
    abstract fun getDisplayData(): List<String>
}

class Thermometer : BluetoothDev("Thermometer") {
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        Log.e("Bluetooth", "Byte array: ${characteristicValue?.toHexString()}")
        val resultMap = mutableMapOf<String, Any>()
        characteristicValue?.let {

            val flagResult = parseSettingsFromByte(characteristicValue[0])

            if(flagResult != null) {
                val temp = parseTemperatureFromByte(characteristicValue.copyOfRange(1, 5))
                temp?.let { temperature ->
                    val unit = if (flagResult.isTemperatureInCelsius == true) "°C" else "°F"
                    resultMap["Temperatura"] = "$temperature $unit"
                }

                if (flagResult.isTimestampPresent) {
                    val timestamp = parseTimestampFromByte(characteristicValue.copyOfRange(5, 12))
                    timestamp?.let { resultMap["Data pomiaru"] = it.time }
                } else {
                    //Log.d("No flag")
                }

                /*if (flagResult.isTemperatureTypePresent) {
                    val measurePlace = parseMeasurePlaceFromByte(characteristicValue[12])
                    measurePlace?.let { resultMap["Miejsce pomiaru"] = it }
                } else {

                }*/

            } else {
                val temp = parseTemperatureFromByte(characteristicValue.copyOfRange(1, 5))
                temp?.let { temperature ->
                    val unit = "°C"
                    resultMap["Temperatura"] = "$temperature $unit"
                }
            }
        }
        return resultMap
    }

    override fun getDisplayData(): List<String> {
        return listOf("Temperatura", "Data pomiaru", "Miejsce pomiaru")
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

// Rozszerzenie dla ByteArray
fun ByteArray.toHexString(): String {
    return joinToString(separator = " ") { byte -> "%02X".format(byte) }
}