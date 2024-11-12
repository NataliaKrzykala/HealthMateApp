package com.example.healthmate.ble

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.healthmate.R
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
import java.time.format.DateTimeFormatter
import java.util.UUID

abstract class BluetoothDev {
    private var _name: String = ""

    // Publiczny getter do odczytu
    val name: String
        get() = _name

    // Publiczny setter do ustawienia
    @Composable
    open fun setName(newName: String) {
        _name = newName
    }

    @Composable
    abstract fun parseData(characteristicValue: ByteArray?): Map<String, Any>
    @Composable
    abstract fun getDisplayData(): List<String>
}

class Thermometer : BluetoothDev() {

    @Composable
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        Log.e("Bluetooth", "Byte array: ${characteristicValue?.toHexString()}")
        val resultMap = mutableMapOf<String, Any>()
        characteristicValue?.let {

            val flagResult = parseTemperatureFlag(characteristicValue[0])

            if(flagResult != null) {
                val temp = parseTemperatureFromByte(characteristicValue.copyOfRange(1, 5))
                temp?.let { temperature ->
                    val unit = if (flagResult.isTemperatureInCelsius == true) "°C" else "°F"
                    resultMap[stringResource(R.string.temperature)] = "$temperature $unit"
                    resultMap["TempResult"] = temperature
                    resultMap["Unit"] = "$unit"
                }

                if (flagResult.isTimestampPresent) {
                    val timestamp = parseTimestampFromByte(characteristicValue.copyOfRange(5, 12))
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")
                    val formattedDateTime = timestamp?.format(formatter)
                    formattedDateTime?.let { resultMap[stringResource(R.string.time_of_measurement)] = formattedDateTime}
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
                    resultMap[stringResource(R.string.temperature)] = "$temperature $unit"
                    resultMap["TempResult"] = temperature
                    resultMap["Unit"] = "$unit"
                }
            }
        }
        return resultMap
    }

    @Composable
    override fun getDisplayData(): List<String> {
        return listOf(stringResource(R.string.temperature), stringResource(R.string.time_of_measurement)) //"Miejsce pomiaru"
    }
}

class WeightScale : BluetoothDev() {
    @Composable
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        Log.e("Bluetooth", "Weight scale: ${characteristicValue?.toHexString()}")
        val resultMap = mutableMapOf<String, Any>()
        characteristicValue?.let {

            val flagResult = parseWeightScaleFlag(characteristicValue[0])

            if(flagResult != null) {
                val weightMeas = parseWeightMeasurement(characteristicValue.copyOfRange(1, 3), flagResult.isInKilograms)
                weightMeas?.let { weight ->
                    val unit = if (flagResult.isInKilograms == true) "kg" else "lb"
                    resultMap[stringResource(R.string.weight)] = "$weight $unit"
                    resultMap["WeightResult"] = "$weight"
                    resultMap["Unit"] = "$unit"
                }

                if (flagResult.isTimestampPresent) {
                    val timestamp = parseTimestampFromByte(characteristicValue.copyOfRange(3, 10))
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")
                    val formattedDateTime = timestamp?.format(formatter)
                    formattedDateTime?.let { resultMap[stringResource(R.string.time_of_measurement)] = formattedDateTime}
                } else {
                    //Log.d("No flag")
                }

                /*TODO: isUserIdPresent and isHeightPresent*/

            } else {
                val weightMeas = parseWeightMeasurement(characteristicValue.copyOfRange(1, 3), true)
                weightMeas?.let { weight ->
                    val unit = "kg"
                    resultMap[stringResource(R.string.weight)] = "$weight $unit"
                    resultMap["WeightResult"] = "$weight"
                    resultMap["Unit"] = "$unit"
                }
            }
        }
        return resultMap
    }

    @Composable
    override fun getDisplayData(): List<String> {
        return listOf(stringResource(R.string.weight), stringResource(R.string.time_of_measurement))
    }
}

class BloodPressureMonitor : BluetoothDev() {
    @Composable
    override fun parseData(characteristicValue: ByteArray?): Map<String, Any> {
        Log.e("Bluetooth", "Blood Pressure Monitor: ${characteristicValue?.toHexString()}")
        val resultMap = mutableMapOf<String, Any>()
        characteristicValue?.let {

            val flagResult = parseBPMFlag(characteristicValue[0])

            if (flagResult != null) {

                val SYSMeas = parseSYSMeasurement(characteristicValue.copyOfRange(1, 3), flagResult.isInmmHg)
                SYSMeas?.let { SYS ->
                    val unit = if (flagResult.isInmmHg == true) "mmHg" else "kPa"
                    resultMap["SYS"] = "$SYS $unit"
                    resultMap["SYSResult"] = SYS
                    resultMap["SYSUnit"] = "$unit"
                }

                val DIAMeas = parseDIAMeasurement(characteristicValue.copyOfRange(3, 5), flagResult.isInmmHg)
                DIAMeas?.let { DIA ->
                    val unit = if (flagResult.isInmmHg == true) "mmHg" else "kPa"
                    resultMap["DIA"] = "$DIA $unit"
                    resultMap["DIAResult"] = DIA
                    resultMap["DIAUnit"] = "$unit"
                }

                val MAPMeas = parseMAPMeasurement(characteristicValue.copyOfRange(5, 7), flagResult.isInmmHg)
                MAPMeas?.let { MAP ->
                    val unit = if (flagResult.isInmmHg == true) "mmHg" else "kPa"
                    resultMap["MAP"] = "$MAP $unit"
                    resultMap["MAPResult"] = MAP
                    resultMap["MAPUnit"] = "$unit"
                }

                if (flagResult.isTimestampPresent) {
                    val timestamp = parseTimestampFromByte(characteristicValue.copyOfRange(7, 14))
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")
                    val formattedDateTime = timestamp?.format(formatter)
                    formattedDateTime?.let { resultMap[stringResource(R.string.time_of_measurement)] = formattedDateTime}
                }

                if (flagResult.isPulsePresent) {
                    val PulseMeas = parsePulseMeasurement(characteristicValue.copyOfRange(14, 16))
                    PulseMeas?.let { Pulse ->
                        val unit = "/min"
                        resultMap[stringResource(R.string.pulse)] = "$Pulse $unit"
                        resultMap["PulseResult"] = Pulse
                        resultMap["PulseUnit"] = "$unit"
                    }
                }

                if (flagResult.isMeasStatusFlagPresent) {
                    //val byteArray = characteristicValue.takeLast(2).toByteArray()
                    val measStatusFlagResult = parseMeasStatusFlag(characteristicValue[16])

                    if (measStatusFlagResult != null) {
                        if(measStatusFlagResult.noBodyMovement) {
                            measStatusFlagResult?.let { flag0 ->
                                resultMap[stringResource(R.string.body_movement_detected)] = stringResource(R.string.no)
                            }
                        } else {
                            measStatusFlagResult?.let { flag1 ->
                                resultMap[stringResource(R.string.body_movement_detected)] = stringResource(R.string.yes)
                            }
                        }

                        if(measStatusFlagResult.cuffsFitProperly) {
                            measStatusFlagResult?.let { flag2 ->
                                resultMap[stringResource(R.string.cuffs_fit)] = stringResource(R.string.yes)
                            }
                        } else {
                            measStatusFlagResult?.let { flag3 ->
                                resultMap[stringResource(R.string.cuffs_fit)] = stringResource(R.string.no)
                            }
                        }

                        if(measStatusFlagResult.noIrregularPulseDetected) {
                            measStatusFlagResult?.let { flag4 ->
                                resultMap[stringResource(R.string.irregular_pulse_detected)] = stringResource(R.string.no)
                            }
                        } else {
                            measStatusFlagResult?.let { flag5 ->
                                resultMap[stringResource(R.string.irregular_pulse_detected)] = stringResource(R.string.yes)
                            }
                        }

                        if(measStatusFlagResult.pulseStatus == 0) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap[stringResource(R.string.pulse_status)] = stringResource(R.string.proper)
                            }
                        } else if(measStatusFlagResult.pulseStatus == 1) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap[stringResource(R.string.pulse_status)] = stringResource(R.string.over_the_proper)
                            }
                        } else if(measStatusFlagResult.pulseStatus == 2) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap[stringResource(R.string.pulse_status)] = stringResource(R.string.below_the_proper)
                            }
                        } else if(measStatusFlagResult.pulseStatus == 3) {
                            measStatusFlagResult?.let { flag6 ->
                                resultMap[stringResource(R.string.pulse_status)] = stringResource(R.string.no_info)
                            }
                        }

                        if(measStatusFlagResult.properMeasurementPosition) {
                            measStatusFlagResult?.let { flag7 ->
                                resultMap[stringResource(R.string.proper_meas_position)] = stringResource(R.string.yes)
                            }
                        } else {
                            measStatusFlagResult?.let { flags ->
                                resultMap[stringResource(R.string.proper_meas_position)] = stringResource(R.string.no)
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

    @Composable
    override fun getDisplayData(): List<String> {
        return listOf("SYS", "DIA", "MAP", stringResource(R.string.time_of_measurement),
            stringResource(R.string.pulse), stringResource(R.string.body_movement_detected), stringResource(R.string.cuffs_fit),
            stringResource(R.string.irregular_pulse_detected), stringResource(R.string.pulse_status),
            stringResource(R.string.proper_meas_position))
    }
}

// Rozszerzenie dla ByteArray
fun ByteArray.toHexString(): String {
    return joinToString(separator = " ") { byte -> "%02X".format(byte) }
}