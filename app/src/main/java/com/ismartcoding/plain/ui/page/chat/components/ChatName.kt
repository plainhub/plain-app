package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.extensions.formatTime
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.theme.secondaryTextColor

@Composable
fun ChatName(m: VChat) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = m.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(
            text = m.createdAt.formatTime(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.secondaryTextColor),
        )
    }
}
