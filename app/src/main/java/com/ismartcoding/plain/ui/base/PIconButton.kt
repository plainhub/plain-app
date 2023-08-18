package com.ismartcoding.plain.ui.base

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIconButton(
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = LocalContentColor.current,
    showBadge: Boolean = false,
    isHaptic: Boolean? = true,
    isSound: Boolean? = true,
    onClick: () -> Unit = {},
) {
    val view = LocalView.current

    IconButton(
        modifier = containerModifier,
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
                            .offset(x = (-3).dp, y = 9.dp)
                            .clip(CircleShape),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            ) {
                Icon(
                    modifier = modifier,
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = tint,
                )
            }
        } else {
            Icon(
                modifier = modifier,
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }
}