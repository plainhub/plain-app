package com.ismartcoding.plain.ui.base

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIconButton(
    icon: Any,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    showBadge: Boolean = false,
    isHaptic: Boolean? = false,
    isSound: Boolean? = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val view = LocalView.current
    IconButton(
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors().copy(
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = 0.38f)
        ),
        onClick = {
            if (isHaptic == true) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (isSound == true) view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
        },
    ) {
        if (showBadge) {
            BadgedBox(
                badge = {
                    Badge(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = (1).dp, y = (-4).dp)
                            .clip(CircleShape),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                }
            ) {
                PIcon(
                    modifier = Modifier.size(iconSize),
                    icon = icon,
                    contentDescription = contentDescription,
                )
            }
        } else {
            PIcon(
                modifier = Modifier.size(iconSize),
                icon = icon,
                contentDescription = contentDescription,
            )
        }
    }
}
