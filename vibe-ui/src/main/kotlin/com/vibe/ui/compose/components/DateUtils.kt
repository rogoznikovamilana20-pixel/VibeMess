package com.vibe.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString.take(19)) ?: return dateString
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance()
            calendar.time = date

            when {
                isSameDay(calendar, today) -> "Сегодня"
                isYesterday(calendar, today) -> "Вчера"
                isSameYear(calendar, today) -> {
                    val outputFormat = SimpleDateFormat("d MMMM", Locale("ru"))
                    outputFormat.format(date)
                }
                else -> {
                    val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
                    outputFormat.format(date)
                }
            }
        } catch (_: Exception) { dateString }
    }

    fun formatTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString.take(19)) ?: return ""
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (_: Exception) { "" }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = cal2.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(cal1, yesterday)
    }

    private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
}

@Composable
fun DateHeader(dateString: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = DateUtils.formatDate(dateString),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
