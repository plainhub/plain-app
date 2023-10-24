package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp

@Composable
fun DisplayText(
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 24.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 24.dp),
                style =
                MaterialTheme.typography.displaySmall.copy(
                    baselineShift = BaselineShift.Superscript,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
