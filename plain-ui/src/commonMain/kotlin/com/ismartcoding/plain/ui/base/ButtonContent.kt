package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize

@Composable
internal fun ButtonContent(text: String, icon: Painter?, size: ButtonSize, loading: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(if (loading) 0f else 1f),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (size == ButtonSize.SMALL) 16.dp else 20.dp))
                HorizontalSpace(8.dp)
            }
            Text(text, style = size.textStyle(), fontWeight = size.fontWeight())
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (size == ButtonSize.SMALL) 18.dp else 24.dp).clearAndSetSemantics {},
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        }
    }
}
