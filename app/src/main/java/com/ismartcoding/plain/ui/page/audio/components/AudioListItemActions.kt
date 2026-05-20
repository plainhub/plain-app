package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.MaterialTheme
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AudioListItemActions(
    item: DAudio,
    audioPlaylistVM: AudioPlaylistViewModel,
    castMode: Boolean,
    castItems: List<IMedia>,
    isInPlaylist: Boolean,
    iconResource: DrawableResource,
    iconColor: androidx.compose.ui.graphics.Color,
    rotation: Float,
    onAnimStart: () -> Unit,
    onAnimEnd: () -> Unit,
) {
    val context = LocalContext.current
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
                scope.launch(Dispatchers.IO) {
                    onAnimStart()
                    if (isInCastQueue) CastPlayer.removeItem(item) else CastPlayer.addItem(item)
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
                scope.launch(Dispatchers.IO) {
                    onAnimStart()
                    if (isInPlaylist) audioPlaylistVM.removeAsync(context, item.path) else audioPlaylistVM.addAsync(context, listOf(item))
                    delay(400)
                    onAnimEnd()
                }
            }
        )
    }
}
