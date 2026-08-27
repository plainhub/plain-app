package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.share_link
import com.ismartcoding.plain.i18n.share_link_copied
import org.jetbrains.compose.resources.stringResource

// The corner copy button is a small circle whose center sits exactly on the
// card's bottom-right corner, so ~50% overlaps the card and ~50% floats outside.
private val CornerButtonSize = 48.dp

/**
 * Card showing [text] with a circular copy button attached to its bottom-right
 * corner like a floating action button. The wrapper reserves room for the half
 * of the button that hangs outside the card, so the card never clips it.
 */
@Composable
fun CornerCopyCard(
    text: String,
    modifier: Modifier = Modifier,
    clipLabel: String = stringResource(Res.string.share_link),
    copiedMessage: String = stringResource(Res.string.share_link_copied),
) {
    val halfButton = CornerButtonSize / 2
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = halfButton, bottom = halfButton),
    ) {
        PCard(horizontal = 0.dp) {
            PListItem(
                title = text,
                modifier = Modifier.padding(end = halfButton),
            )
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
            CopyIconButton(
                text = text,
                clipLabel = clipLabel,
                copiedMessage = copiedMessage,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(CornerButtonSize)
                    .offset(x = halfButton, y = halfButton)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
