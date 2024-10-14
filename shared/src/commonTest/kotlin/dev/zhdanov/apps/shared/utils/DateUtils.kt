package dev.zhdanov.apps.shared.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class DateUtils {

    @Test
    fun testStartOfDayWithoutShift() {
        // Given
        val time = Instant.parse("2024-10-10T15:30:00Z") // Arbitrary time
        val timeZone = TimeZone.of("Europe/London") // London Time Zone

        // When
        val result = startOfDayWithShift(time, timeZone)

        // Expected start of the day in the provided time zone (London)
        val expected = Instant.parse("2024-10-09T23:00:00Z")

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun testStartOfDayWithPositiveShift() {
        // Given
        val time = Instant.parse("2024-10-09T22:00:00Z")
        val timeZone = TimeZone.of("Asia/Tbilisi") // Tbilisi Time Zone (UTC+4)
        val shift = 4.hours

        // When
        val result = startOfDayWithShift(time, timeZone, shift)

        // Expected start of the day in Tbilisi time zone (without applying shift)
        val expected = Instant.parse("2024-10-09T00:00:00Z")

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun testStartOfDayWithNegativeShift() {
        // Given
        val time = Instant.parse("2024-10-10T15:30:00Z")
        val timeZone = TimeZone.of("Europe/Madrid") // Madrid Time Zone (UTC+2 during DST)
        val shift = (-2).hours

        // When
        val result = startOfDayWithShift(time, timeZone, shift)

        // Expected start of the day in Madrid time zone (without applying shift)
        val expected = Instant.parse("2024-10-09T20:00:00Z")

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun testStartOfDayInDifferentTimeZone() {
        // Given
        val time = Instant.parse("2024-10-10T15:30:00Z")
        val timeZone = TimeZone.of("Asia/Tokyo") // Tokyo Time Zone (UTC+9)

        // When
        val result = startOfDayWithShift(time, timeZone)

        // Expected start of the day in Tokyo time zone (without applying shift)
        val expected = Instant.parse("2024-10-10T15:00:00Z") // 00:00 Tokyo corresponds to 15:00 UTC on the previous day

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun testStartOfDayWithShiftAndDifferentTimeZone() {
        // Given
        val time = Instant.parse("2024-10-10T15:30:00Z")
        val timeZone = TimeZone.of("America/New_York") // New York Time Zone (UTC-4)
        val shift = 5.hours

        // When
        val result = startOfDayWithShift(time, timeZone, shift)

        // Expected start of the day in New York time zone
        val expected = Instant.parse("2024-10-10T09:00:00Z") // Start of day in New York

        // Then
        assertEquals(expected, result)
    }
}
