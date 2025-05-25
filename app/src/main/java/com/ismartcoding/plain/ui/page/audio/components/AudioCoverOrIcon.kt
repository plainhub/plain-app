package com.ismartcoding.plain.ui.page.audio.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import java.util.concurrent.ConcurrentHashMap


val audioCoverCache = ConcurrentHashMap<String, ImageBitmap>()
val EMPTY_BITMAP = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()

@Composable
fun AudioCoverOrIcon(
    path: String?,
    modifier: Modifier = Modifier
) {
    if (path.isNullOrBlank()) {
        Icon(
            painter = painterResource(R.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        return
    }

    var imageBitmap by remember(path) {
        mutableStateOf<ImageBitmap?>(
            audioCoverCache[path]?.takeIf { it != EMPTY_BITMAP }
        )
    }
    var isLoading by remember(path) {
        mutableStateOf(!audioCoverCache.containsKey(path))
    }

    LaunchedEffect(path) {
        if (audioCoverCache.containsKey(path)) {
            val cached = audioCoverCache[path]
            imageBitmap = if (cached == EMPTY_BITMAP) null else cached
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val bitmap = withIO {
            try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(path)
                    retriever.embeddedPicture?.let {
                        BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                    }
                } finally {
                    retriever.release()
                }
            } catch (e: Exception) {
                LogCat.e("Failed to load cover for: $e, $path")
                null
            }
        }

        audioCoverCache[path] = bitmap ?: EMPTY_BITMAP
        imageBitmap = bitmap
        isLoading = false
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
