package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultCategoryFilter
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CategoryFilterChips(
    selectedCategory: VaultCategoryFilter,
    onCategorySelected: (VaultCategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VaultCategoryFilter.values().forEach { category ->
            val isSelected = selectedCategory == category
            val icon: ImageVector = when (category) {
                VaultCategoryFilter.ALL -> Icons.Default.AllInclusive
                VaultCategoryFilter.PHOTOS -> Icons.Default.Image
                VaultCategoryFilter.VIDEOS -> Icons.Default.Videocam
                VaultCategoryFilter.DOCUMENTS -> Icons.Default.Description
                VaultCategoryFilter.TEXT -> Icons.Default.EditNote
                VaultCategoryFilter.OTHER -> Icons.Default.FolderZip
            }

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.label,
                        fontSize = 12.sp,
                        color = if (isSelected) Color(0xFF00363D) else TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color(0xFF00363D) else CyberCyan
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurface,
                    selectedContainerColor = CyberCyan,
                    labelColor = TextPrimary,
                    selectedLabelColor = Color(0xFF00363D)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF263353),
                    selectedBorderColor = CyberCyan,
                    enabled = true,
                    selected = isSelected
                ),
                modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
            )
        }
    }
}
