package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.painterResource

/**
 * Material input chip with an optional leading icon and an optional close
 * action in the trailing slot. The close hit area is a 20dp circle inside the
 * chip; clicks on it do not fall through to [onClick].
 */
@Composable
fun PInputChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    closable: Boolean = false,
    closeContentDescription: String? = null,
    onClose: () -> Unit = {},
) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = { Text(text = text) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (icon != null) {
            {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else null,
        trailingIcon = if (closable) {
            {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    PIcon(
                        icon = painterResource(Res.drawable.close),
                        contentDescription = closeContentDescription,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else null,
        colors = InputChipDefaults.inputChipColors().copy(
            containerColor = MaterialTheme.colorScheme.cardBackgroundNormal,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
    )
}
