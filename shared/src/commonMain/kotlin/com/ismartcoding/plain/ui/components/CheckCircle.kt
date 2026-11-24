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
import androidx.compose.ui.state.ToggleableState
import com.ismartcoding.plain.ui.resources.horizontal_rule
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
fun CheckCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) = CheckCircle(ToggleableState(selected), modifier, onClick)

@Composable
internal fun CheckCircle(
    state: ToggleableState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val marked = state != ToggleableState.Off
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (marked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(width = 1.5.dp, color = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape = CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (marked) {
            Icon(
                painter = painterResource(if (state == ToggleableState.On) UiRes.drawable.ui_drawable_check else UiRes.drawable.horizontal_rule),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
