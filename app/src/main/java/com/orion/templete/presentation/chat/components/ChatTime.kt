package com.orion.templete.presentation.chat.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// Instagram-style time labels. All inputs are epoch millis.
object ChatTime {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR
    private const val WEEK = 7 * DAY

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val fullDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    // Inbox: "now", "5m", "2h", "3d", "2w", then "12 Sep"
    fun short(time: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - time).coerceAtLeast(0)
        return when {
            diff < MINUTE -> "now"
            diff < HOUR -> "${diff / MINUTE}m"
            diff < DAY -> "${diff / HOUR}h"
            diff < WEEK -> "${diff / DAY}d"
            diff < 5 * WEEK -> "${diff / WEEK}w"
            else -> dayMonth.format(Instant.ofEpochMilli(time).atZone(zone))
        }
    }

    // Thread header: "Active now", "Active 5m ago", "Active 3h ago", "Active yesterday", "Active 3d ago"; null = unknown
    fun activeStatus(lastActive: Long?, now: Long = System.currentTimeMillis()): String? {
        if (lastActive == null || lastActive <= 0) return null
        val diff = (now - lastActive).coerceAtLeast(0)
        return when {
            diff < 3 * MINUTE -> "Active now"
            diff < HOUR -> "Active ${diff / MINUTE}m ago"
            diff < DAY -> "Active ${diff / HOUR}h ago"
            diff < 2 * DAY -> "Active yesterday"
            diff < WEEK -> "Active ${diff / DAY}d ago"
            else -> null
        }
    }

    // Centered separator between message groups: "Today 10:42", "Yesterday 21:03", "Mon 11:00", "12 Sep 2026, 11:00"
    fun separator(time: Long, now: Long = System.currentTimeMillis()): String {
        val at = Instant.ofEpochMilli(time).atZone(zone)
        val days = ChronoUnit.DAYS.between(at.toLocalDate(), LocalDate.now(zone))
        val clock = timeFormat.format(at)
        return when {
            days <= 0L -> "Today $clock"
            days == 1L -> "Yesterday $clock"
            days < 7L -> "${weekday.format(at)} $clock"
            else -> "${fullDate.format(at)}, $clock"
        }
    }

    // "Seen" receipt: "Seen" within a minute, otherwise "Seen 5m ago"
    fun seen(readAt: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - readAt).coerceAtLeast(0)
        return if (diff < MINUTE) "Seen" else "Seen ${short(readAt, now)} ago"
    }
}
