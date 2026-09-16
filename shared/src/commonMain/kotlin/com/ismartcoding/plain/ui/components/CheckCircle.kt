package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.check

@Composable
fun CheckCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(width = 1.5.dp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape = CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
