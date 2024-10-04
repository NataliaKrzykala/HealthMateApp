package com.example.healthmate.ui

import java.util.Calendar

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

/*fun parseSettingsFromByte(byteArray: ByteArray?): String? {
    if (byteArray == null) return null

    val place = byteArray[14]
}*/

/*fun parseTimestampFromDec(yearLSB: Int, yearMSB: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): LocalDateTime {
    // Łączymy dwa bajty roku
    val year = (yearMSB shl 8) or yearLSB

    // Zwróć LocalDateTime
    return LocalDateTime.of(year, month, day, hour, minute, second)
}

fun parseTimestampFromDec(
    yearLSB: Int,
    yearMSB: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int
): Calendar {
    // Łączymy dwa bajty roku
    val year = (yearMSB shl 8) or yearLSB

    // Utwórz Calendar i ustaw wartości
    val calendar = Calendar.getInstance()

    // Miesiące w Calendar są indeksowane od 0, więc musimy odjąć 1
    calendar.set(year, month - 1, day, hour, minute, second)

    return calendar
}

fun parseTimestampFromByte(byteArray: ByteArray): LocalDateTime {
    // Łączymy dwa bajty roku
    val year = ((byteArray[1].toInt() and 0xFF) shl 8) or (byteArray[0].toInt() and 0xFF)
    val month = byteArray[2].toInt() and 0xFF
    val day = byteArray[3].toInt() and 0xFF
    val hour = byteArray[4].toInt() and 0xFF
    val minute = byteArray[5].toInt() and 0xFF
    val second = byteArray[6].toInt() and 0xFF

    // Zwróć LocalDateTime
    return LocalDateTime.of(year, month, day, hour, minute, second)
}

fun parseTemperatureFromDec(msb: Int, middle: Int, lsb: Int, exponent: Int): Float {
    // Połącz MSB, middle, LSB w mantysę
    val mantissa = (msb shl 16) or (middle shl 8) or lsb

    // Obliczenie temperatury zgodnie z formułą: mantissa * 10^exponent
    val result = mantissa * Math.pow(10.0, exponent.toDouble())
    return result.toFloat()
}*/

