package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.models.VClickText

@Composable
fun DisplayText(
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
    clickTexts: List<VClickText> = listOf()
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 0.dp,
                    end = 24.dp,
                    bottom = 16.dp,
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
            val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (clickTexts.isEmpty()) {
                Text(
                    text = description,
                    style = style,
                )
            } else {
                PClickableText(
                    text = description,
                    clickTexts,
                    style = style,
                )
            }
        }
    }
}
