package com.ismartcoding.plain.ui.page.scan.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.green
import org.jetbrains.compose.resources.painterResource

private val TagSize = 44.dp
private val TagIconSize = 24.dp

/** The green circular tap marker pinned on one detected code, WeChat-style. */
@Composable
fun ScanCodeTag(
    index: Int,
    modifier: Modifier = Modifier,
    onPick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(TagSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.green)
            .border(2.dp, Color.White, CircleShape)
            .clickable { onPick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.arrow_right),
            contentDescription = (index + 1).toString(),
            tint = Color.White,
            modifier = Modifier.size(TagIconSize),
        )
    }
}

/** Half the tag size in px for centering markers on a point; pair with [ScanCodeTag]. */
fun scanCodeTagRadiusPx(density: androidx.compose.ui.unit.Density): Float = with(density) { TagSize.toPx() } / 2f
