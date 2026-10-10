package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.grey

// Generic single-choice selector rendered as a row of dots. The selected
// item grows horizontally and uses primary; others stay short and use grey.
// Each dot owns a fixed 24.dp tall touch target so tapping is comfortable
// regardless of the visible dot size. Pair with PSegmentedButtons when text
// labels are required; dots fit two-to-four item selectors with no room for
// words.
@Composable
fun <T> PRangeDots(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val on = item == selected
            val color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.grey
            Box(
                modifier = Modifier
                    .height(DOT_TOUCH_HEIGHT)
                    .selectable(
                        selected = on,
                        role = Role.Tab,
                        onClick = { onSelect(item) },
                    )
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = if (on) 16.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

private val DOT_TOUCH_HEIGHT = 24.dp