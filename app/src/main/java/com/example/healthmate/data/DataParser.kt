package com.example.healthmate.data

import java.time.DateTimeException
import java.time.LocalDateTime

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
    if (byteArray.size != 4) throw IllegalArgumentException("Invalid byte array: too short/long")


    val mantissa = ((byteArray[2].toInt() and 0xFF) shl 16) or
            ((byteArray[1].toInt() and 0xFF) shl 8) or
            (byteArray[0].toInt() and 0xFF)

    val exponent = byteArray[3].toInt()

    val result = mantissa * Math.pow(10.0, exponent.toDouble())
    return result.toFloat()
}

fun parseTimestampFromByte(byteArray: ByteArray?): LocalDateTime? {
    if(byteArray == null) return null

    if (byteArray.size != 7) {
        throw IllegalArgumentException("Invalid timestamp format: byte array too short/long")
    }

    val year = ((byteArray[0].toInt() and 0xFF) or ((byteArray[1].toInt() and 0xFF) shl 8))
    val month = (byteArray[2].toInt() and 0xFF)
    val day = byteArray[3].toInt() and 0xFF
    val hour = byteArray[4].toInt() and 0xFF
    val minute = byteArray[5].toInt() and 0xFF
    val second = byteArray[6].toInt() and 0xFF

    if (year > LocalDateTime.now().year) throw DateTimeException("Invalid year: $year is in the future")
    if (month !in 1..12) throw DateTimeException("Invalid month value: $month")
    if (day !in 1..31) throw DateTimeException("Invalid day value: $day")
    if (hour !in 0..23) throw DateTimeException("Invalid hour value: $hour")
    if (minute !in 0..59) throw DateTimeException("Invalid minute value: $minute")
    if (second !in 0..59) throw DateTimeException("Invalid second value: $second")

    return try {
        LocalDateTime.of(year, month, day, hour, minute, second)
    } catch (e: DateTimeException) {
        throw DateTimeException("Invalid date: ${e.message}")
    }
}

fun convertTimestampToByteArray(dateTime: LocalDateTime): ByteArray {
    val year = dateTime.year
    val month = dateTime.monthValue
    val day = dateTime.dayOfMonth
    val hour = dateTime.hour
    val minute = dateTime.minute
    val second = dateTime.second

    // Tworzenie tablicy bajtów o odpowiednim rozmiarze
    val byteArray = ByteArray(7) // 2 + 1 + 1 + 1 + 1 + 1 = 7 bajtów

    // Konwersja roku (2 bajty w LSB)
    byteArray[0] = (year and 0xFF).toByte()        // LSB
    byteArray[1] = (year shr 8 and 0xFF).toByte() // MSB
    // Miesiąc
    byteArray[2] = month.toByte()
    // Dzień
    byteArray[3] = day.toByte()
    // Godzina
    byteArray[4] = hour.toByte()
    // Minuta
    byteArray[5] = minute.toByte()
    // Sekunda
    byteArray[6] = second.toByte()

    return byteArray
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

fun parseWeightMeasurement(byteArray: ByteArray?, isInKilograms: Boolean): Float? {
    if(byteArray == null) return null
    if (byteArray.size != 2) throw IllegalArgumentException("Invalid byte array: too short/long")

    val weightValue: Int = (byteArray[1].toInt() and 0xFF shl 8) or (byteArray[0].toInt() and 0xFF)

    return if(isInKilograms) {
        // Przeliczenie wartości na kg z rozdzielczością 0.005 kg - /**TODO - ZMIENIĆ?/
        (weightValue * 0.005).toFloat()
    } else {
        (weightValue * 0.01).toFloat()
    }
}

// Blood Pressure Monitor

data class BPMFlag(
    val isInmmHg: Boolean,
    val isTimestampPresent: Boolean,
    val isPulsePresent: Boolean,
    val isUserIdPresent: Boolean,
    val isMeasStatusFlagPresent: Boolean
)
fun parseBPMFlag(flagByte: Byte?): BPMFlag? {
    if(flagByte == null) return null

    val isInmmHg = (flagByte.toInt() and 0x01) == 0
    val isTimestampPresent = (flagByte.toInt() shr 1 and 0x01) == 1
    val isPulsePresent = (flagByte.toInt() shr 2 and 0x01) == 1
    val isUserIdPresent = (flagByte.toInt() shr 3 and 0x01) == 1
    val isMeasStatusFlagPresent = (flagByte.toInt() shr 4 and 0x01) == 1

    return BPMFlag(
        isInmmHg = isInmmHg,
        isTimestampPresent = isTimestampPresent,
        isPulsePresent = isPulsePresent,
        isUserIdPresent = isUserIdPresent,
        isMeasStatusFlagPresent = isMeasStatusFlagPresent
    )
}

fun parseSYSMeasurement(byteArray: ByteArray?, isInmmHg: Boolean): Float? {
    if(byteArray == null) return null
    if (byteArray.size != 2) throw IllegalArgumentException("Invalid byte array: too short/long")

    val mantissa = (byteArray[1].toInt() and 0xF0 shl 8) or (byteArray[0].toInt() and 0xFF)
    val exponent = byteArray[1].toInt() and 0x0F

    val result = if (isInmmHg) {
        // Wartości w mmHg - mantysa * 10^exponent
        mantissa * Math.pow(10.0, exponent.toDouble())
    } else {
        // Wartości w kPa - zastosowanie wzoru: R = C × M × 10^d × 2^b
        val M = 1      // Mnożnik
        val d = 3      // Eksponent dziesiętny
        val b = 0      // Eksponent binarny
        mantissa * M * Math.pow(10.0, d.toDouble()) * Math.pow(2.0, b.toDouble()) / 1000 // podzielić na 1000 bo to wynik w Pa?
    }

    return result.toFloat()
}

fun parseDIAMeasurement(byteArray: ByteArray?, isInmmHg: Boolean): Float? {
    if(byteArray == null) return null
    if (byteArray.size != 2) throw IllegalArgumentException("Invalid byte array: too short/long")

    val mantissa = (byteArray[1].toInt() and 0xF0 shl 8) or (byteArray[0].toInt() and 0xFF)
    val exponent = byteArray[1].toInt() and 0x0F

    val result = if (isInmmHg) {
        // Wartości w mmHg - mantysa * 10^exponent
        mantissa * Math.pow(10.0, exponent.toDouble())
    } else {
        // Wartości w kPa - zastosowanie wzoru: R = C × M × 10^d × 2^b
        val M = 1      // Mnożnik
        val d = 3      // Eksponent dziesiętny
        val b = 0      // Eksponent binarny
        mantissa * M * Math.pow(10.0, d.toDouble()) * Math.pow(2.0, b.toDouble()) / 1000 // podzielić na 1000 bo to wynik w Pa?
    }

    return result.toFloat()

}

fun parseMAPMeasurement(byteArray: ByteArray?, isInmmHg: Boolean): Float? {
    if(byteArray == null) return null
    if (byteArray.size != 2) throw IllegalArgumentException("Invalid byte array: too short/long")

    val mantissa = (byteArray[1].toInt() and 0xF0 shl 8) or (byteArray[0].toInt() and 0xFF)
    val exponent = byteArray[1].toInt() and 0x0F

    val result = if (isInmmHg) {
        // Wartości w mmHg - mantysa * 10^exponent
        mantissa * Math.pow(10.0, exponent.toDouble())
    } else {
        // Wartości w kPa - zastosowanie wzoru: R = C × M × 10^d × 2^b
        val M = 1      // Mnożnik
        val d = 3      // Eksponent dziesiętny
        val b = 0      // Eksponent binarny
        mantissa * M * Math.pow(10.0, d.toDouble()) * Math.pow(2.0, b.toDouble()) / 1000 // podzielić na 1000 bo to wynik w Pa?
    }

    return result.toFloat()
}

fun parsePulseMeasurement(byteArray: ByteArray?): Float? {
    if(byteArray == null) return null
    if (byteArray.size != 2) throw IllegalArgumentException("Invalid byte array: too short/long")

    val mantissa = (byteArray[1].toInt() and 0xF0 shl 8) or (byteArray[0].toInt() and 0xFF)

    val exponent = byteArray[1].toInt() and 0x0F

    val result = mantissa * Math.pow(10.0, exponent.toDouble())
    return result.toFloat()
}

data class MeasStatusFlag(
    val noBodyMovement: Boolean,
    val cuffsFitProperly: Boolean,
    val noIrregularPulseDetected: Boolean,
    val pulseStatus: Int, //?
    val properMeasurementPosition: Boolean
)
fun parseMeasStatusFlag(flagByte: Byte?): MeasStatusFlag? {
    if(flagByte == null) return null

    //val number = flagByte.toInt() and 0xFF

    val noBodyMovement = (flagByte.toInt() and 0x01) == 0
    val cuffsFitProperly = (flagByte.toInt() shr 1 and 0x01) == 0
    val noIrregularPulseDetected = (flagByte.toInt() shr 2 and 0x01) == 0

    val thirdBit = (flagByte.toInt() shr 3 and 0x01)
    val fourthBit = (flagByte.toInt() shr 4 and 0x01)
    val pulseStatus = ((thirdBit shl 1) or fourthBit)

    val properMeasurementPosition = (flagByte.toInt() shr 4 and 0x01) == 0

    return MeasStatusFlag(
        noBodyMovement = noBodyMovement,
        cuffsFitProperly = cuffsFitProperly,
        noIrregularPulseDetected = noIrregularPulseDetected,
        pulseStatus = pulseStatus,
        properMeasurementPosition = properMeasurementPosition,
    )
}