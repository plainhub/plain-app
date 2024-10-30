package com.ismartcoding.plain.ui.page.files.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.models.BreadcrumbItem


@Composable
fun BreadcrumbView(
    breadcrumbs: List<BreadcrumbItem>,
    selectedIndex: Int,
    onItemClick: (BreadcrumbItem) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, item ->
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { onItemClick(item) }
                    .padding(vertical = 4.dp)
            )

            if (index < breadcrumbs.size - 1) {
                Text(
                    text = " > ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 