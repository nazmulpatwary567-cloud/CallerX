package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    fun formatRelativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diff = nowMs - timestampMs
        if (diff < 0) return "Just now"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            seconds < 45 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 && isSameDay(timestampMs, nowMs) -> "${hours}h ago"
            isYesterday(timestampMs, nowMs) -> "Yesterday"
            diff < TimeUnit.DAYS.toMillis(7) -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestampMs))
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
        }
    }

    fun formatFullDateTime(timestampMs: Long): String {
        return SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(timestampMs))
    }

    fun formatTimeOnly(timestampMs: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(ts: Long, nowMs: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = ts }
        val c2 = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
