package com.ismartcoding.plain.ui.base

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.badgeBorderColor
import com.ismartcoding.plain.ui.theme.red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIconButton(
    icon: Int,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    showBadge: Boolean = false,
    isHaptic: Boolean? = false,
    isSound: Boolean? = false,
    enabled: Boolean = true,
    click: () -> Unit = {},
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
            click()
        },
    ) {
        if (showBadge) {
            BadgedBox(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .offset(x = 4.dp, y = (-6).dp)
                            .background(MaterialTheme.colorScheme.badgeBorderColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.red, shape = CircleShape)
                        )
                    }
                }
            ) {
                PIcon(
                    modifier = Modifier.size(iconSize),
                    icon = painterResource(icon),
                    contentDescription = contentDescription,
                )
            }
        } else {
            PIcon(
                modifier = Modifier.size(iconSize),
                icon = painterResource(icon),
                contentDescription = contentDescription,
            )
        }
    }
}




