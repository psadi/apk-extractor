package com.psadi.apkextractor.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psadi.apkextractor.data.model.AppCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterTabs(
    selectedCategory: AppCategory,
    userCount: Int,
    systemCount: Int,
    extractedCount: Int,
    onCategorySelected: (AppCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        SegmentedButton(
            selected = selectedCategory == AppCategory.USER,
            onClick = { onCategorySelected(AppCategory.USER) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            label = {
                Text(
                    text = "Installed ($userCount)",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        )

        SegmentedButton(
            selected = selectedCategory == AppCategory.SYSTEM,
            onClick = { onCategorySelected(AppCategory.SYSTEM) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            label = {
                Text(
                    text = "System ($systemCount)",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        )

        SegmentedButton(
            selected = selectedCategory == AppCategory.EXTRACTED,
            onClick = { onCategorySelected(AppCategory.EXTRACTED) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            label = {
                Text(
                    text = "Extracted ($extractedCount)",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        )
    }
}
