package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.MaterialTheme
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AudioListItemActions(
    item: DAudio,
    castMode: Boolean,
    castItems: List<IMedia>,
    isInPlaylist: Boolean,
    iconResource: DrawableResource,
    iconColor: Color,
    rotation: Float,
    onAnimStart: () -> Unit,
    onAnimEnd: () -> Unit,
    onCastToggle: suspend (DAudio, Boolean) -> Unit,
    onPlaylistToggle: suspend (DAudio, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (castMode) {
        val isInCastQueue = remember(item.path, castItems) {
            castItems.any { it.path == item.path }
        }
        PIconButton(
            icon = if (isInCastQueue) Res.drawable.playlist_remove else Res.drawable.playlist_add,
            tint = if (isInCastQueue) MaterialTheme.colorScheme.red else MaterialTheme.colorScheme.primary,
            contentDescription = if (isInCastQueue) stringResource(Res.string.remove_from_cast_queue) else stringResource(Res.string.add_to_cast_queue),
            modifier = Modifier.rotate(rotation),
            click = {
                scope.launch(Dispatchers.Default) {
                    onAnimStart()
                    onCastToggle(item, isInCastQueue)
                    delay(400)
                    onAnimEnd()
                }
            }
        )
    } else {
        PIconButton(
            icon = iconResource,
            tint = iconColor,
            contentDescription = if (isInPlaylist) stringResource(Res.string.remove_from_playlist) else stringResource(Res.string.add_to_playlist),
            modifier = Modifier.rotate(rotation),
            click = {
                scope.launch(Dispatchers.Default) {
                    onAnimStart()
                    onPlaylistToggle(item, isInPlaylist)
                    delay(400)
                    onAnimEnd()
                }
            }
        )
    }
}
