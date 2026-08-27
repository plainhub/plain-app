package com.ismartcoding.plain.ui.base

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.check
import com.ismartcoding.plain.i18n.copy
import com.ismartcoding.plain.i18n.copy_text
import com.ismartcoding.plain.lib.extensions.toBreakableUrl
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The corner copy button is a small circle whose center sits exactly on the
// card's bottom-right corner, so ~50% overlaps the card and ~50% floats outside.
private val CornerButtonSize = 32.dp

// How long the copy button shows the check icon before switching back.
private const val COPIED_ICON_DURATION_MS = 1500L

@Composable
fun CornerCopyCard(
    label: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    val halfButton = CornerButtonSize / 2
    // Incremented on every copy so the check icon shows for the full duration again.
    var copiedTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(copiedTick) {
        if (copiedTick > 0) {
            delay(COPIED_ICON_DURATION_MS)
            copiedTick = 0
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = halfButton),
    ) {
        PCard {
            Text(
                modifier = Modifier.padding(16.dp),
                text = text.toBreakableUrl(),
                style = MaterialTheme.typography.listItemTitle(),
            )
        }
        IconButton(
            onClick = {
                setClipboardText(label, text)
                copiedTick++
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(CornerButtonSize)
                .offset(y = halfButton)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AnimatedContent(
                targetState = copiedTick > 0,
                transitionSpec = {
                    (scaleIn(tween(150)) + fadeIn(tween(150))) togetherWith
                            (scaleOut(tween(150)) + fadeOut(tween(150)))
                },
                label = "cornerCopyIcon",
            ) { copied ->
                PIcon(
                    modifier = Modifier.size(16.dp),
                    icon = painterResource(if (copied) Res.drawable.check else Res.drawable.copy),
                    contentDescription = stringResource(Res.string.copy_text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
