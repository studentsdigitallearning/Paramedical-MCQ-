package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DailyTestTimeUtils {

    const val TIMEZONE_IST = "Asia/Kolkata"
    const val APP_WEBSITE_URL = ""
    const val YOUTUBE_URL = ""
    const val TELEGRAM_URL = ""
    const val INSTAGRAM_URL = ""

    val istTimeZone: TimeZone = TimeZone.getTimeZone(TIMEZONE_IST)

    fun getCurrentDateKey(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = istTimeZone
        }
        return format.format(Date())
    }

    fun getFormattedDisplayDate(dateKey: String = getCurrentDateKey()): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = istTimeZone
            }
            val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US).apply {
                timeZone = istTimeZone
            }
            val parsed = parser.parse(dateKey)
            if (parsed != null) formatter.format(parsed) else dateKey
        } catch (_: Exception) {
            dateKey
        }
    }

    fun getMillisUntilMidnightIST(): Long {
        val now = Calendar.getInstance(istTimeZone)
        val midnight = Calendar.getInstance(istTimeZone).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = midnight.timeInMillis - now.timeInMillis
        return maxOf(0L, diff)
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }

    fun formatTimeTaken(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun getCurrentMonthKey(): String {
        val format = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = istTimeZone
        }
        return format.format(Date())
    }

    fun getCurrentMonthDisplayName(): String {
        val format = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = istTimeZone
        }
        return format.format(Date())
    }

    fun getCurrentDayOfMonth(): Int {
        val cal = Calendar.getInstance(istTimeZone)
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun getDaysInCurrentMonth(): Int {
        val cal = Calendar.getInstance(istTimeZone)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getFirstDayOfWeekOffsetInMonth(): Int {
        val cal = Calendar.getInstance(istTimeZone).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // Sunday is 1 in Calendar -> offset 0, Monday 2 -> offset 1, etc.
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    /**
     * Generates a temporary daily test roll number for participants.
     * Format: "YYMMDDNNNN"
     * Example: "2610090001"
     */
    fun generateTemporaryRollNo(dateKey: String = getCurrentDateKey(), sequenceNumber: Int = 1): String {
        val parts = dateKey.split("-")
        val yy = if (parts.isNotEmpty() && parts[0].length >= 4) parts[0].substring(2, 4) else "26"
        val mm = if (parts.size >= 2) parts[1].padStart(2, '0') else "10"
        val dd = if (parts.size >= 3) parts[2].padStart(2, '0') else "09"
        val seqStr = String.format(Locale.US, "%04d", sequenceNumber.coerceAtLeast(1))
        return "$yy$mm$dd$seqStr"
    }
}


