package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R

@Composable
fun ActionButtons(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        content = content
    )
}

@Composable
fun ActionButtonMore(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.ellipsis_vertical,
        contentDescription = stringResource(R.string.more),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonMoreWithMenu(content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    PIconButton(
        icon = R.drawable.ellipsis_vertical,
        contentDescription = stringResource(R.string.more),
        tint = MaterialTheme.colorScheme.onSurface,
        click = {
            isMenuOpen = true
        },
    )
    PDropdownMenu(
        expanded = isMenuOpen,
        onDismissRequest = { isMenuOpen = false }
    ) {
        content {
            isMenuOpen = false
        }
    }
}

@Composable
fun ActionButtonAdd(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.plus,
        contentDescription = stringResource(R.string.add),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonRefresh(
    onClick: () -> Unit,
    loading: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )
    
    PIconButton(
        icon = R.drawable.refresh_ccw,
        contentDescription = stringResource(R.string.refresh),
        tint = if (loading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = if (loading) Modifier.rotate(rotation) else Modifier,
        click = onClick,
    )
}


@Composable
fun ActionButtonSettings(
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    PIconButton(
        icon = R.drawable.settings,
        contentDescription = stringResource(R.string.settings),
        tint = MaterialTheme.colorScheme.onSurface,
        showBadge = showBadge,
        click = onClick,
    )
}

@Composable
fun ActionButtonSelect(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.list_checks,
        contentDescription = stringResource(R.string.select),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonTags(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.tag,
        contentDescription = stringResource(R.string.tags),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSort(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.sort,
        contentDescription = stringResource(R.string.sort),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}


@Composable
fun ActionButtonSearch(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.search,
        contentDescription = stringResource(R.string.search),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}


@Composable
fun ActionButtonFolderKanban(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.folder_kanban,
        contentDescription = stringResource(R.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonFolders(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.folder,
        contentDescription = stringResource(R.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonCast(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.cast,
        contentDescription = stringResource(R.string.cast),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}