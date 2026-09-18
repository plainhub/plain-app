package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PCapsuleMoreClose(
    onClose: () -> Unit,
    onMore: (() -> Unit)? = null,
    moreMenu: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    val tint = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier.height(36.dp)
            .background(MaterialTheme.colorScheme.cardBackgroundNormal, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
    if (isSheetOpen) {
        PModalBottomSheet(
            modifier = Modifier,
            onDismissRequest = { isSheetOpen = false },
        ) {
            Column {
                VerticalSpace(16.dp)
                PSheetActionCard {
                    moreMenu { isSheetOpen = false }
                }
                BottomSpace()
            }
        }
    }
}
