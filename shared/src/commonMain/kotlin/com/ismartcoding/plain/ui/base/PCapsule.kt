package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * WeChat-mini-program style capsule: translucent pill with a thin outline, two
 * sections separated by a vertical divider. Left = "more" menu (opens a bottom
 * sheet), right = "close". Both inner buttons are vertically centered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PCapsuleMoreClose(
    onClose: () -> Unit,
    showMore: Boolean = true,
    onMore: (() -> Unit)? = null,
    moreMenu: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    val tint = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(percent = 50)
    // No capsule outline when the more button is hidden — the close dot stands alone
    val boxModifier = if (showMore) {
        Modifier.clip(shape).border(width = 1.dp, color = tint.copy(alpha = 0.15f), shape = shape)
    } else {
        Modifier
    }
    Box(modifier = boxModifier) {
        Row(
            modifier = Modifier.height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showMore) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onMore?.invoke() ?: run { isSheetOpen = true } },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.more_three_dots),
                        contentDescription = stringResource(Res.string.more),
                        modifier = Modifier.padding(horizontal = 9.dp).size(24.dp),
                        colorFilter = ColorFilter.tint(tint),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(tint.copy(alpha = 0.2f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.circle_dot),
                    contentDescription = stringResource(Res.string.close),
                    modifier = Modifier.padding(horizontal = 9.dp).size(24.dp),
                    colorFilter = ColorFilter.tint(tint),
                )
            }
        }
    }
    if (isSheetOpen) {
        PModalBottomSheet(
            modifier = Modifier,
            onDismissRequest = { isSheetOpen = false },
        ) {
            moreMenu { isSheetOpen = false }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tint.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { isSheetOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.cancel),
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
