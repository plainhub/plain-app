package com.ismartcoding.plain.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.os.Parcelable
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.util.UnstableApi
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayOutputStream
import java.io.Serializable

@OptIn(UnstableApi::class)
@Parcelize
@kotlinx.serialization.Serializable
data class DPlaylistAudio(
    val title: String,
    val path: String,
    val artist: String,
    val duration: Long,
) : Parcelable, Serializable {

    fun toMediaItem(): MediaItem {
        val mediaMetadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artist)
            .setArtist(artist)
            .setMediaType(MEDIA_TYPE_MUSIC)
        
        // Dynamically extract artwork or create default
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(null, path.pathToUri())
            var artworkData = retriever.embeddedPicture
            
            // Create default artwork if none exists
            if (artworkData == null) {
                artworkData = createDefaultArtwork()
            }
            
            if (artworkData != null) {
                mediaMetadataBuilder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
            retriever.release()
        } catch (e: Exception) {
            // Create default artwork on error
            try {
                val defaultArtwork = createDefaultArtwork()
                if (defaultArtwork != null) {
                    mediaMetadataBuilder.setArtworkData(defaultArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            } catch (ex: Exception) {
                // Ignore default artwork creation errors
            }
        }
        
        return MediaItem.Builder()
            .setUri(path.pathToUri())
            .setMediaId(path)
            .setCustomCacheKey(path)
            .setMediaMetadata(mediaMetadataBuilder.build())
            .build()
    }

    private fun createDefaultArtwork(): ByteArray? {
        return try {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Modern gradient background
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.parseColor("#667eea"), // Soft blue
                Color.parseColor("#764ba2"), // Soft purple
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            
            // Music note symbol
            paint.shader = null
            paint.color = Color.WHITE
            paint.alpha = 200
            paint.textSize = size * 0.4f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            
            val musicSymbol = "♪"
            val x = size / 2f
            val y = size / 2f + paint.textSize / 3f
            canvas.drawText(musicSymbol, x, y, paint)
            
            // Convert to byte array
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            bitmap.recycle()
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun getSubtitle(): String {
        return listOf(artist, duration.formatDuration()).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    companion object {
        fun fromPath(
            context: Context,
            path: String,
        ): DPlaylistAudio {
            val retriever = MediaMetadataRetriever()
            var title = path.getFilenameWithoutExtensionFromPath()
            var duration = 0L
            var artist = getString(R.string.unknown)
            
            try {
                retriever.setDataSource(context, path.pathToUri())
                val keyTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
                if (keyTitle.isNotEmpty()) {
                    title = keyTitle
                }
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                val keyArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                if (keyArtist.isNotEmpty()) {
                    artist = keyArtist
                }
                retriever.release()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            return DPlaylistAudio(title, path, artist, duration / 1000)
        }

        private const val serialVersionUID = -11L
    }
}
