package com.ismartcoding.plain.ui.base

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.platform.rememberClipboardWriter
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import com.ismartcoding.plain.ui.resources.copy_text as ui_string_copy_text
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// How long the button shows the check icon before switching back.
private const val COPIED_ICON_DURATION_MS = 1500L

@Composable
fun CopyIconButton(
    text: String,
    clipLabel: String,
    modifier: Modifier = Modifier,
    icon: DrawableResource = UiRes.drawable.ui_drawable_copy,
    contentDescription: String = stringResource(UiRes.string.ui_string_copy_text),
    onCopied: (() -> Unit)? = null,
) {
    // Incremented on every copy so the check icon shows for the full duration again.
    var copiedTick by remember { mutableIntStateOf(0) }
    val writeClipboard = rememberClipboardWriter()
    LaunchedEffect(copiedTick) {
        if (copiedTick > 0) {
            delay(COPIED_ICON_DURATION_MS)
            copiedTick = 0
        }
    }
    IconButton(
        modifier = modifier,
        onClick = {
            writeClipboard(clipLabel, text)
            copiedTick++
            onCopied?.invoke()
        },
    ) {
        AnimatedContent(
            targetState = copiedTick > 0,
            transitionSpec = {
                (scaleIn(tween(150)) + fadeIn(tween(150))) togetherWith
                        (scaleOut(tween(150)) + fadeOut(tween(150)))
            },
            label = "copyIcon",
        ) { copied ->
            PIcon(
                modifier = Modifier.size(24.dp),
                icon = painterResource(if (copied) UiRes.drawable.ui_drawable_check else icon),
                contentDescription = contentDescription,
            )
        }
    }
}
