package com.example.healthmate.ui

import java.util.Calendar
data class TemperatureFlag(
    val isTemperatureInCelsius: Boolean, // True jeśli temperatura w Celsjuszach, False jeśli w Fahrenheitach
    val isTimestampPresent: Boolean,   // True jeśli timestamp obecne
    val isTemperatureTypePresent: Boolean // True jeśli pole typu temperatury obecne
)
fun parseTemperatureFlag(flagByte: Byte?): TemperatureFlag? {
    if(flagByte == null) return null

    val isTemperatureInCelsius = (flagByte.toInt() and 0x01) == 0 // Ostatni bit
    val isTimestampPresent = (flagByte.toInt() shr 1 and 0x01) == 1 // Przedostatni bit
    val isTemperatureTypePresent = (flagByte.toInt() shr 2 and 0x01) == 1 // Przedprzedostatni bit

    return TemperatureFlag(
        isTemperatureInCelsius = isTemperatureInCelsius,
        isTimestampPresent = isTimestampPresent,
        isTemperatureTypePresent = isTemperatureTypePresent
    )
}
fun parseTemperatureFromByte(byteArray: ByteArray?): Float? {
    if(byteArray == null) return null
    val mantissa = ((byteArray[2].toInt() and 0xFF) shl 16) or
            ((byteArray[1].toInt() and 0xFF) shl 8) or
            (byteArray[0].toInt() and 0xFF)

    val exponent = byteArray[3].toInt()

    val result = mantissa * Math.pow(10.0, exponent.toDouble())
    return result.toFloat()
}

fun parseTimestampFromByte(byteArray: ByteArray?): Calendar? {
    if(byteArray == null) return null

    val year = ((byteArray[1].toInt() and 0xFF) shl 8) or (byteArray[0].toInt() and 0xFF)
    val month = (byteArray[2].toInt() and 0xFF) - 1
    val day = byteArray[3].toInt() and 0xFF
    val hour = byteArray[4].toInt() and 0xFF
    val minute = byteArray[5].toInt() and 0xFF
    val second = byteArray[6].toInt() and 0xFF

    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, hour, minute, second)

    return calendar
}

/*fun parseMeasurePlaceFromByte(measurePlaceByte: Byte?): String? {
    if(measurePlaceByte == null) return null
    return when (measurePlaceByte.toInt() and 0xFF) {
        0x01 -> "Pacha"
        0x02 -> "Ciało"
        0x03 -> "Ucho"
        0x04 -> "Palec"
        0x05 -> "Przewód pokarmowy"
        0x06 -> "Usta"
        0x07 -> "Odbyt"
        0x08 -> "Palec u nogi"
        0x09 -> "Błona bębenkowa"
        else -> "Nieznane"
    }
}*/

data class WeightScaleFlag(
    val isInKilograms: Boolean,
    val isTimestampPresent: Boolean,
    val isUserIdPresent: Boolean,
    val isHeightPresent: Boolean
)
fun parseWeightScaleFlag(flagByte: Byte?): WeightScaleFlag? {
    if(flagByte == null) return null

    val isInKilograms = (flagByte.toInt() and 0x01) == 0
    val isTimestampPresent = (flagByte.toInt() shr 1 and 0x01) == 1
    val isUserIdPresent = (flagByte.toInt() shr 2 and 0x01) == 1
    val isHeightPresent = (flagByte.toInt() shr 3 and 0x01) == 1

    return WeightScaleFlag(
        isInKilograms = isInKilograms,
        isTimestampPresent = isTimestampPresent,
        isUserIdPresent = isUserIdPresent,
        isHeightPresent = isHeightPresent
    )
}

fun parseWeightMeasurement(byteArray: ByteArray?): Float? {
    if(byteArray == null) return null

    val weightValue: Int = (byteArray[1].toInt() and 0xFF shl 8) or (byteArray[0].toInt() and 0xFF)

    // Przeliczenie wartości na kg z rozdzielczością 0.005 kg - /**TODO - ZMIENIĆ?/
    return (weightValue * 0.005).toFloat()
}

