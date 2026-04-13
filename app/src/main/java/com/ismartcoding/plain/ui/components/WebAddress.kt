package com.ismartcoding.plain.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun HttpHttpsSegmentedButton(isHttps: Boolean, onSelect: (Boolean) -> Unit) {
    val trackColor = MaterialTheme.colorScheme.cardBackgroundNormal
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedText = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedText = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .padding(3.dp),
    ) {
        listOf(false to "HTTP", true to "HTTPS").forEach { (https, label) ->
            val selected = isHttps == https
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) selectedColor else trackColor)
                    .clickable { onSelect(https) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) selectedText else unselectedText,
                )
            }
        }
    }
}