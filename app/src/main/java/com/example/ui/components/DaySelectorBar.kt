package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Doctor
import com.example.ui.theme.TealPrimary

@Composable
fun DaySelectorBar(
    selectedDay: String?,
    todayArabic: String,
    onDaySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Virtualized horizontal list rendering (LazyRow) for high performance and smooth scrolling
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All Days" option
        item(key = "all_days") {
            val isAllSelected = selectedDay == null
            FilterChip(
                selected = isAllSelected,
                onClick = { onDaySelected(null) },
                label = {
                    Text(
                        text = "جميع الأيام",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealPrimary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_day_all")
            )
        }

        // Weekdays - Virtualized with unique stable keys
        items(Doctor.ALL_DAYS, key = { it }) { day ->
            val isSelected = selectedDay == day
            val isToday = day == todayArabic
            FilterChip(
                selected = isSelected,
                onClick = { onDaySelected(day) },
                label = {
                    Text(
                        text = if (isToday) "$day (اليوم)" else day,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealPrimary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("filter_day_$day")
            )
        }
    }
}
