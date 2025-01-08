package com.example.healthmate

import com.example.healthmate.data.BPMFlag
import com.example.healthmate.data.MeasStatusFlag
import com.example.healthmate.data.TemperatureFlag
import com.example.healthmate.data.WeightScaleFlag
import com.example.healthmate.data.parseBPMFlag
import com.example.healthmate.data.parseDIAMeasurement
import com.example.healthmate.data.parseMAPMeasurement
import com.example.healthmate.data.parseMeasStatusFlag
import com.example.healthmate.data.parsePulseMeasurement
import com.example.healthmate.data.parseSYSMeasurement
import com.example.healthmate.data.parseTemperatureFlag
import com.example.healthmate.data.parseTemperatureFromByte
import com.example.healthmate.data.parseTimestampFromByte
import com.example.healthmate.data.parseWeightMeasurement
import com.example.healthmate.data.parseWeightScaleFlag
import org.junit.Test

import org.junit.Assert.*
import org.junit.jupiter.api.assertThrows
import java.time.DateTimeException

import java.time.LocalDateTime

class DataParserTest {

    @Test
    fun `parseTemperatureFlag returns null for null input`() {
        val result = parseTemperatureFlag(null)
        assertNull(result)
    }

    @Test
    fun `parseTemperatureFlag correctly parses all flags enabled`() {
        val flagByte: Byte = 0b00000111
        val expected = TemperatureFlag(
            isTemperatureInCelsius = false,
            isTimestampPresent = true,
            isTemperatureTypePresent = true
        )

        val result = parseTemperatureFlag(flagByte)

        assertEquals(expected, result)
    }

    @Test
    fun `parseTemperatureFlag correctly parses all flags disabled`() {
        val flagByte: Byte = 0b00000000
        val expected = TemperatureFlag(
            isTemperatureInCelsius = true,
            isTimestampPresent = false,
            isTemperatureTypePresent = false
        )

        val result = parseTemperatureFlag(flagByte)
        assertEquals(expected, result)
    }

    @Test
    fun `parseTemperatureFlag correctly parses flag with mixed bits`() {
        val flagByte: Byte = 0b00000110
        val expected = TemperatureFlag(
            isTemperatureInCelsius = true,
            isTimestampPresent = true,
            isTemperatureTypePresent = true
        )

        val result = parseTemperatureFlag(flagByte)
        assertEquals(expected, result)
    }

    @Test
    fun `parseTemperatureFromByte should return null for null input`() {
        val result = parseTemperatureFromByte(null)
        assertNull(result)
    }

    @Test
    fun `parseTemperatureFromByte should throw exception for invalid input`() {
        val input = byteArrayOf(0x01, 0x02)

        val exception = assertThrows<IllegalArgumentException> {
            parseTemperatureFromByte(input)
        }
        assertEquals("Invalid byte array: too short/long", exception.message)
    }

    @Test
    fun `parseTemperatureFromByte should return 0 for zero mantissa`() {
        val input = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val result = parseTemperatureFromByte(input)
        assertEquals(0.0f, result)
    }

    @Test
    fun `parseTemperatureFromByte should correctly parse positive temperature`() {
        val input = byteArrayOf(0x6E, 0x01, 0x00, 0xFF.toByte()) // Mantysa = 366, Wykładnik = -1
        val result = parseTemperatureFromByte(input)
        assertEquals(36.6f, result)
    }

    @Test
    fun `parseTimestampFromByte should return null for null byte array`() {
        assertNull(parseTimestampFromByte(null))
    }

    @Test
    fun `parseTimestampFromByte should throw exception for too short byte array`() {
        val invalidBytes = byteArrayOf(0x01, 0x02)

        val exception = assertThrows<IllegalArgumentException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid timestamp format: byte array too short/long", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw exception for too long byte array`() {
        val invalidBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x09)
        val exception = assertThrows<IllegalArgumentException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid timestamp format: byte array too short/long", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should correctly parse valid byte array`() {
        val inputBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0C, 0x1F, 0x10, 0x20, 0x00)
        val expectedTimestamp = LocalDateTime.of(2021, 12, 31, 16, 32, 0)

        val result = parseTimestampFromByte(inputBytes)

        assertEquals(expectedTimestamp, result)
    }

    @Test
    fun `parseTimestampFromByte should throw exception for invalid month`() {
        val invalidBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0D, 0x1F, 0x10, 0x20, 0x00)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid month value: 13", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw exception for invalid day`() {
        val invalidBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0C, 0x20, 0x10, 0x20, 0x00)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid day value: 32", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw exception for invalid hour`() {
        val invalidBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0C, 0x1F, 0x19, 0x20, 0x00)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid hour value: 25", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw exception for invalid minute`() {
        val invalidBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0C, 0x1F, 0x10, 0x3C, 0x00)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid minute value: 60", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw exception for invalid second`() {
        val invalidBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0C, 0x1F, 0x10, 0x20, 0x3C)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(invalidBytes)
        }
        assertEquals("Invalid second value: 60", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw DateTimeException for future year`() {
        val futureYearBytes = byteArrayOf(0xE3.toByte(), 0x0B, 0x01, 0x01, 0x00, 0x00, 0x00)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(futureYearBytes)
        }
        assertEquals("Invalid year: 3043 is in the future", exception.message)
    }

    @Test
    fun `parseTimestampFromByte should throw DateTimeException for invalid day in month`() {
        val invalidDayBytes = byteArrayOf(0xE5.toByte(), 0x07, 0x0B, 0x1F, 0x00, 0x00, 0x00)

        val exception = assertThrows<DateTimeException> {
            parseTimestampFromByte(invalidDayBytes)
        }
        assertEquals("Invalid date: Invalid date 'NOVEMBER 31'", exception.message)
    }

    @Test
    fun `parseWeightScaleFlag should return null for null input`() {
        val input: Byte? = null
        val result = parseWeightScaleFlag(input)
        assertEquals(null, result)
    }

    @Test
    fun `parseWeightScaleFlag should correctly parse flag with all bits off`() {
        val input: Byte? = 0x00
        val expected = WeightScaleFlag(
            isInKilograms = true,
            isTimestampPresent = false,
            isUserIdPresent = false,
            isHeightPresent = false
        )
        val result = parseWeightScaleFlag(input)
        assertEquals(expected, result)
    }

    @Test
    fun `parseWeightScaleFlag should correctly parse flag with all bits on`() {
        val input: Byte? = 0x0F
        val expected = WeightScaleFlag(
            isInKilograms = false,
            isTimestampPresent = true,
            isUserIdPresent = true,
            isHeightPresent = true
        )
        val result = parseWeightScaleFlag(input)
        assertEquals(expected, result)
    }

    @Test
    fun `parseWeightScaleFlag should correctly parse flag with mixed bits`() {
        val input: Byte? = 0x05
        val expected = WeightScaleFlag(
            isInKilograms = false,
            isTimestampPresent = false,
            isUserIdPresent = true,
            isHeightPresent = false
        )
        val result = parseWeightScaleFlag(input)
        assertEquals(expected, result)
    }

    @Test
    fun `parseWeightMeasurement should return null for null input`() {
        val input: ByteArray? = null
        val isInKilograms = true
        val result = parseWeightMeasurement(input, isInKilograms)
        assertEquals(null, result)
    }

    @Test
    fun `parseWeightMeasurement should throw exception for too short byte array`() {
        val invalidBytes = byteArrayOf(0x01)
        val isInKilograms = true

        val exception = assertThrows<IllegalArgumentException> {
            parseWeightMeasurement(invalidBytes, isInKilograms)
        }
        assertEquals("Invalid byte array: too short/long", exception.message)
    }

    @Test
    fun `parseWeightMeasurement should correctly parse weight in kilograms`() {
        val input = byteArrayOf(0x64, 0x00) // 0x0064 = 100
        val isInKilograms = true
        val expected = 0.5f // 100 * 0.005
        val result = parseWeightMeasurement(input, isInKilograms)
        assertEquals(expected, result)
    }

    @Test
    fun `parseWeightMeasurement should correctly parse weight in pounds`() {
        val input = byteArrayOf(0x64, 0x00) // 0x0064 = 100
        val isInKilograms = false
        val expected = 1.0f // 100 * 0.01
        val result = parseWeightMeasurement(input, isInKilograms)
        assertEquals(expected, result)
    }

    @Test
    fun `parseWeightMeasurement should correctly parse weight with decimal value (in kilograms)`() {
        val input = byteArrayOf(0x6C, 0x2B) // 0x2B6C = 11100
        val isInKilograms = true
        val expected = 55.58f
        val result = parseWeightMeasurement(input, isInKilograms)
        assertEquals(expected, result)
    }

    @Test
    fun `parseBPMFlag should return null for null input`() {
        val result = parseBPMFlag(null)
        assertNull(result)
    }

    @Test
    fun `parseBPMFlag should correctly parse flag`() {
        val flagByte = 0b00010110.toByte()
        val expected = BPMFlag(
            isInmmHg = true,
            isTimestampPresent = true,
            isPulsePresent = true,
            isUserIdPresent = false,
            isMeasStatusFlagPresent = true
        )

        val result = parseBPMFlag(flagByte)
        assertEquals(expected, result)
    }

    @Test
    fun `parseSYSMeasurement should return null for null input`() {
        val result = parseSYSMeasurement(null, true)
        assertNull(result)
    }

    @Test
    fun `parseSYSMeasurement should throw exception for too short byte array`() {
        val invalidBytes = byteArrayOf(0x01)
        val isInMmHg = true

        val exception = assertThrows<IllegalArgumentException> {
            parseSYSMeasurement(invalidBytes, isInMmHg)
        }
        assertEquals("Invalid byte array: too short/long", exception.message)
    }

    @Test
    fun `parseSYSMeasurement should correctly parse SYS value in mmHg`() {
        val input = byteArrayOf(0xB4.toByte(), 0x00.toByte()) // 0x00B4 = 180
        val isInmmHg = true
        val expected = 180.0f // Mantysa 180, exponent 0
        val result = parseSYSMeasurement(input, isInmmHg)

        assertEquals(expected, result)
    }

    @Test
    fun `parseDIAMeasurement should return null for null input`() {
        val result = parseDIAMeasurement(null, true)
        assertNull(result)
    }

    @Test
    fun `parseDIAMeasurement should throw exception for too short byte array`() {
        val invalidBytes = byteArrayOf(0x01)
        val isInMmHg = true

        val exception = assertThrows<IllegalArgumentException> {
            parseDIAMeasurement(invalidBytes, isInMmHg)
        }
        assertEquals("Invalid byte array: too short/long", exception.message)
    }

    @Test
    fun `parseDIAMeasurement should correctly parse value in mmHg`() {
        val input = byteArrayOf(0xB4.toByte(), 0x00.toByte())
        val isInmmHg = true
        val expected = 180.0f
        val result = parseDIAMeasurement(input, isInmmHg)

        assertEquals(expected, result)
    }

    @Test
    fun `parseMAPMeasurement should return null for null input`() {
        val result = parseMAPMeasurement(null, false)
        assertNull(result)
    }

    @Test
    fun `parseMAPMeasurement should throw exception for too short byte array`() {
        val invalidBytes = byteArrayOf(0x01)
        val isInMmHg = true

        val exception = assertThrows<IllegalArgumentException> {
            parseMAPMeasurement(invalidBytes, isInMmHg)
        }
        assertEquals("Invalid byte array: too short/long", exception.message)
    }

    @Test
    fun `parseMAPMeasurement should correctly parse value in mmHg`() {
        val input = byteArrayOf(0xB4.toByte(), 0x00.toByte())
        val isInmmHg = true
        val expected = 180.0f
        val result = parseMAPMeasurement(input, isInmmHg)

        assertEquals(expected, result)
    }

    @Test
    fun `parsePulseMeasurement should return null for null input`() {
        val result = parsePulseMeasurement(null)
        assertNull(result)
    }

    @Test
    fun `parsePulseMeasurement should throw exception for too short byte array`() {
        val invalidBytes = byteArrayOf(0x01)

        val exception = assertThrows<IllegalArgumentException> {
            parsePulseMeasurement(invalidBytes)
        }
        assertEquals("Invalid byte array: too short/long", exception.message)
    }

    @Test
    fun `parsePulseMeasurement should correctly parse pulse value`() {
        val input = byteArrayOf(0xB4.toByte(), 0x00.toByte())
        val expected = 180.0f
        val result = parsePulseMeasurement(input)

        assertEquals(expected, result)
    }

    @Test
    fun `parseMeasStatusFlag should return null for null input`() {
        val result = parseMeasStatusFlag(null)

        assertNull(result)
    }

    @Test
    fun `parseMeasStatusFlag should return correct flag for valid byte input`() {
        // Przykładowa flaga: 0x00 (00000000)
        val flagByte = 0x00.toByte()

        val result = parseMeasStatusFlag(flagByte)

        val expected = MeasStatusFlag(
            noBodyMovement = true,
            cuffsFitProperly = true,
            noIrregularPulseDetected = true,
            pulseStatus = 0,
            properMeasurementPosition = true
        )

        assertEquals(expected, result)
    }

    @Test
    fun `parseMeasStatusFlag should return correct flag for another valid byte input`() {
        // Przykładowa flaga: 0x1F (00011111)
        val flagByte = 0x1F.toByte()

        val result = parseMeasStatusFlag(flagByte)

        val expected = MeasStatusFlag(
            noBodyMovement = false,
            cuffsFitProperly = false,
            noIrregularPulseDetected = false,
            pulseStatus = 3, // trzeci i czwarty bit ustawiony na 1, więc pulseStatus = 3
            properMeasurementPosition = false
        )

        assertEquals(expected, result)
    }


    // !!!!!!!!! PROBLEMY !!!!!!!!!

    @Test
    fun `parseSYSMeasurement should correctly parse value in kPa`() {
        val input = byteArrayOf(0x18.toByte(), 0x00.toByte())
        val isInmmHg = false
        val expected = 24.0f
        val result = parseSYSMeasurement(input, isInmmHg)

        assertEquals(expected, result)
    }

    @Test
    fun `parseDIAMeasurement should correctly parse value in kPa`() {
        val input = byteArrayOf(0x10, 0x00.toByte())
        val isInmmHg = false
        val expected = 16.0f
        val result = parseDIAMeasurement(input, isInmmHg)

        assertEquals(expected, result)
    }

    @Test
    fun `parseMAPMeasurement should correctly parse value in kPa`() {
        val input = byteArrayOf(0x0B, 0x00.toByte())
        val isInmmHg = false
        val expected = 11.0f
        val result = parseMAPMeasurement(input, isInmmHg)

        assertEquals(expected, result)
    }

    /*TODO: dodatkowe testy wartości granicznych? Czy funkcje powinny być pod to dostosowane?*/
}