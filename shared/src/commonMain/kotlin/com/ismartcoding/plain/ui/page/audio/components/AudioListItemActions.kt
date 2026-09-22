package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.MaterialTheme
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.ui.base.PIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Trailing action of [AudioListItem]: toggles manual-queue membership, or
 * cast-queue membership in cast mode. Owns its icon rotation animation.
 */
@Composable
fun AudioListItemActions(
    item: DAudio,
    castMode: Boolean,
    castItems: List<IMedia>,
    isInQueue: Boolean,
    onCastToggle: suspend (DAudio, Boolean) -> Unit,
    onQueueToggle: suspend (DAudio, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var animating by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (animating) 90f else 0f,
        animationSpec = tween(durationMillis = 400), label = "icon_rotation"
    )

    val active: Boolean
    val description: String
    val toggle: suspend (DAudio, Boolean) -> Unit
    if (castMode) {
        active = castItems.any { it.path == item.path }
        description = if (active) stringResource(Res.string.remove_from_cast_queue) else stringResource(Res.string.add_to_cast_queue)
        toggle = onCastToggle
    } else {
        active = isInQueue
        description = if (active) stringResource(Res.string.remove_from_queue) else stringResource(Res.string.add_to_queue)
        toggle = onQueueToggle
    }

    PIconButton(
        icon = if (active) Res.drawable.playlist_remove else Res.drawable.playlist_add,
        tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentDescription = description,
        modifier = Modifier.rotate(rotation),
        click = {
            scope.launch(Dispatchers.Default) {
                animating = true
                toggle(item, active)
                delay(400)
                animating = false
            }
        }
    )
}
