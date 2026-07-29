package com.vibe.common.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility class for date and time operations.
 */
object DateTimeUtils {

    private val defaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Converts a unix timestamp in seconds to a [LocalDateTime].
     */
    fun fromUnixTime(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
    }

    /**
     * Formats a [LocalDateTime] using the default formatter.
     */
    fun format(dateTime: LocalDateTime, pattern: String? = null): String {
        val formatter = if (pattern != null) DateTimeFormatter.ofPattern(pattern) else defaultFormatter
        return dateTime.format(formatter)
    }

    /**
     * Returns the current time in seconds (Unix time).
     */
    fun currentUnixTime(): Long {
        return Instant.now().epochSecond
    }
}
