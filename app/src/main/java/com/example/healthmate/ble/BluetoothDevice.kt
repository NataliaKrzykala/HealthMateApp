package com.example.healthmate.ble

import android.util.Log
import com.example.healthmate.ui.parseBPMFlag
import com.example.healthmate.ui.parseDIAMeasurement
import com.example.healthmate.ui.parseMAPMeasurement
import com.example.healthmate.ui.parseMeasStatusFlag
import com.example.healthmate.ui.parsePulseMeasurement
import com.example.healthmate.ui.parseSYSMeasurement
import com.example.healthmate.ui.parseTemperatureFlag
import com.example.healthmate.ui.parseTemperatureFromByte
import com.example.healthmate.ui.parseTimestampFromByte
import com.example.healthmate.ui.parseWeightMeasurement
import com.example.healthmate.ui.parseWeightScaleFlag
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

            val flagResult = parseTemperatureFlag(characteristicValue[0])

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
        return listOf("Temperatura", "Data pomiaru") //"Miejsce pomiaru"
    }
}

class WeightScale : BluetoothDev("Weight scale") {
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        Log.e("Bluetooth", "Weight scale: ${characteristicValue?.toHexString()}")
        val resultMap = mutableMapOf<String, Any>()
        characteristicValue?.let {

            val flagResult = parseWeightScaleFlag(characteristicValue[0])

            if(flagResult != null) {
                val weightMeas = parseWeightMeasurement(characteristicValue.copyOfRange(1, 3))
                weightMeas?.let { weight ->
                    val unit = if (flagResult.isInKilograms == true) "kg" else "lb"
                    resultMap["Waga"] = "$weight $unit"
                }

                if (flagResult.isTimestampPresent) {
                    val timestamp = parseTimestampFromByte(characteristicValue.copyOfRange(3, 10))
                    timestamp?.let { resultMap["Data pomiaru"] = it.time }
                } else {
                    //Log.d("No flag")
                }

                /*TODO: isUserIdPresent and isHeightPresent*/

            } else {
                val weightMeas = parseWeightMeasurement(characteristicValue.copyOfRange(1, 3))
                weightMeas?.let { weight ->
                    val unit =  "kg"
                    resultMap["Waga"] = "$weight $unit"
                }
            }
       }
        return resultMap
    }

    override fun getDisplayData(): List<String> {
        return listOf("Waga", "Data pomiaru")
    }
}

class BloodPressureMonitor : BluetoothDev("Blood Pressure Monitor") {
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        Log.e("Bluetooth", "Blood Pressure Monitor: ${characteristicValue?.toHexString()}")
        val resultMap = mutableMapOf<String, Any>()
        characteristicValue?.let {

            val flagResult = parseBPMFlag(characteristicValue[0])

            if (flagResult != null) {

                if(flagResult.isInmmHg == true) {

                    val SYSMeas = parseSYSMeasurement(characteristicValue.copyOfRange(1, 3))
                    SYSMeas?.let { SYS ->
                        val unit = "mmHg"
                        resultMap["SYS"] = "$SYS $unit"
                    }

                    val DIAMeas = parseDIAMeasurement(characteristicValue.copyOfRange(3, 5))
                    DIAMeas?.let { DIA ->
                        val unit = "mmHg"
                        resultMap["DIA"] = "$DIA $unit"
                    }

                    val MAPMeas = parseMAPMeasurement(characteristicValue.copyOfRange(5, 7))
                    MAPMeas?.let { MAP ->
                        val unit = "mmHg"
                        resultMap["MAP"] = "$MAP $unit"
                    }

                }

                if (flagResult.isTimestampPresent) {
                    val timestamp = parseTimestampFromByte(characteristicValue.copyOfRange(7, 14))
                    timestamp?.let { resultMap["Data pomiaru"] = it.time }
                }

                if (flagResult.isPulsePresent) {
                    val PulseMeas = parsePulseMeasurement(characteristicValue.copyOfRange(14, 16))
                    PulseMeas?.let { Pulse ->
                        val unit = "/min"
                        resultMap["Pulse"] = "$Pulse $unit"
                    }
                }

                if (flagResult.isMeasStatusFlagPresent) {
                    //val byteArray = characteristicValue.takeLast(2).toByteArray()
                    val measStatusFlagResult = parseMeasStatusFlag(characteristicValue[16])

                    if (measStatusFlagResult != null) {
                        if(measStatusFlagResult.noBodyMovement) {
                            measStatusFlagResult?.let { flag0 ->
                                resultMap["Body movement detected"] = "No"
                            }
                        } else {
                            measStatusFlagResult?.let { flag1 ->
                                resultMap["Body movement detected"] = "Yes"
                            }
                        }

                        if(measStatusFlagResult.cuffsFitProperly) {
                            measStatusFlagResult?.let { flag2 ->
                                resultMap["Cuffs fit"] = "Proper"
                            }
                        } else {
                            measStatusFlagResult?.let { flag3 ->
                                resultMap["Cuffs fit"] = "Improper"
                            }
                        }

                        if(measStatusFlagResult.noIrregularPulseDetected) {
                            measStatusFlagResult?.let { flag4 ->
                                resultMap["Irregular pulse detected"] = "No"
                            }
                        } else {
                            measStatusFlagResult?.let { flag5 ->
                                resultMap["Irregular pulse detected"] = "Yes"
                            }
                        }

                        if(measStatusFlagResult.pulseStatus == 0) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap["Pulse status"] = "Proper"
                            }
                        } else if(measStatusFlagResult.pulseStatus == 1) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap["Pulse status"] = "Over the proper value"
                            }
                        } else if(measStatusFlagResult.pulseStatus == 2) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap["Pulse status"] = "Below the proper value"
                            }
                        } else if(measStatusFlagResult.pulseStatus == 3) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap["Pulse status"] = "No information"
                            }
                        }

                        if(measStatusFlagResult.properMeasurementPosition) {
                            measStatusFlagResult?.let { flag7 ->
                                resultMap["Proper measurement position"] = "Yes"
                            }
                        } else {
                            measStatusFlagResult?.let { flags ->
                                resultMap["Proper measurement position"] = "No"
                            }
                        }
                    }
                } else {

                }

                /*TODO: isUserIdPresent*/

            } else {

            }
        }
        return resultMap
    }

    override fun getDisplayData(): List<String> {
        return listOf("SYS", "DIA", "MAP", "Data pomiaru", "Pulse", "Body movement detected", "Cuffs fit", "Irregular pulse detected", "Pulse status", "Proper measurement position")
    }
}

// Rozszerzenie dla ByteArray
fun ByteArray.toHexString(): String {
    return joinToString(separator = " ") { byte -> "%02X".format(byte) }
}