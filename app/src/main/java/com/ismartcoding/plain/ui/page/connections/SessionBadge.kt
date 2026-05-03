package com.ismartcoding.plain.ui.page.connections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.ui.theme.greenDot

@Composable
fun SessionBadge(m: VSession, isOnline: Boolean) {
    val label = if (m.isCustom) stringResource(R.string.custom_token) else if (isOnline) stringResource(R.string.online) else stringResource(R.string.offline)
    val textColor = if (!m.isCustom && isOnline) Color.White else MaterialTheme.colorScheme.onSurface
    val bgColor = if (!m.isCustom && isOnline) MaterialTheme.colorScheme.greenDot else MaterialTheme.colorScheme.surfaceVariant
    Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}