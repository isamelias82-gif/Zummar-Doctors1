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
import com.example.ui.theme.TealSecondary

@Composable
fun SpecialtyFilterRow(
    selectedSpecialty: String?,
    onSpecialtySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Virtualized horizontal list rendering (LazyRow) for high performance and smooth category navigation
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all_specialties") {
            val isAllSelected = selectedSpecialty == null
            FilterChip(
                selected = isAllSelected,
                onClick = { onSpecialtySelected(null) },
                label = {
                    Text(
                        text = "كافة الاختصاصات",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealSecondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_spec_all")
            )
        }

        // Virtualized category list with stable keys
        items(Doctor.ALL_SPECIALTIES, key = { it }) { specialty ->
            val isSelected = selectedSpecialty == specialty
            FilterChip(
                selected = isSelected,
                onClick = { onSpecialtySelected(if (isSelected) null else specialty) },
                label = {
                    Text(
                        text = specialty,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealSecondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_spec_$specialty")
            )
        }
    }
}
