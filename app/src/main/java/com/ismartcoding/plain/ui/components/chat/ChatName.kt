package com.ismartcoding.plain.ui.components.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.extensions.formatTime
import com.ismartcoding.plain.ui.models.VChat

@Composable
fun ChatName(m: VChat) {
    Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        Text(
            text = m.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = m.createdAt.formatTime(),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
        )
    }
}
