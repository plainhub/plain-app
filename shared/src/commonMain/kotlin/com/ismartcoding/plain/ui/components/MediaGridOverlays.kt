package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.theme.darkMask
import com.ismartcoding.plain.ui.theme.lightMask

@Composable
fun SelectedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.lightMask())
            .aspectRatio(1f)
    )
}

@Composable
fun CastModeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.darkMask())
            .aspectRatio(1f)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp),
            painter = painterResource(Res.drawable.cast),
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

@Composable
fun SelectionCheckbox(
    selected: Boolean,
    modifier: Modifier = Modifier,
    partial: Boolean = false,
    unselectedContainer: Color = MaterialTheme.colorScheme.darkMask(),
    unselectedBorderColor: Color = Color.White,
    onClick: () -> Unit,
) {
    val checked = selected || partial
    val containerColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else unselectedContainer,
        label = "selectionContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Color.Transparent else unselectedBorderColor,
        label = "selectionBorder",
    )
    Box(
        modifier = modifier
            .size(26.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(if (partial) Res.drawable.horizontal_rule else Res.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
fun MediaDateGroupHeader(
    dateLabel: String,
    allSelected: Boolean,
    partialSelected: Boolean,
    showCheckbox: Boolean,
    onSelectGroup: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = dateLabel, style = MaterialTheme.typography.titleSmall)
        if (showCheckbox) {
            HorizontalSpace(dp = 8.dp)
            SelectionCheckbox(
                selected = allSelected,
                partial = partialSelected,
                unselectedContainer = Color.Transparent,
                unselectedBorderColor = MaterialTheme.colorScheme.outline,
                onClick = onSelectGroup,
            )
        }
    }
}

@Composable
fun SizeLabel(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.darkMask()),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp),
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
        )
    }
}
