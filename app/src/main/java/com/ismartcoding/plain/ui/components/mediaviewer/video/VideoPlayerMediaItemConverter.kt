package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.FileDataSource.FileDataSourceException
import androidx.media3.datasource.RawResourceDataSource

@SuppressLint("UnsafeOptInUsageError")
internal fun VideoPlayerMediaItem.toUri(
    context: Context,
): Uri = when (this) {
    is VideoPlayerMediaItem.RawResourceMediaItem -> {
        RawResourceDataSource.buildRawResourceUri(resourceId)
    }

    is VideoPlayerMediaItem.AssetFileMediaItem -> {
        val dataSpec = DataSpec(Uri.parse("asset:///$assetPath"))
        val assetDataSource = AssetDataSource(context)
        try {
            assetDataSource.open(dataSpec)
        } catch (e: AssetDataSource.AssetDataSourceException) {
            e.printStackTrace()
        }

        assetDataSource.uri ?: Uri.EMPTY
    }

    is VideoPlayerMediaItem.NetworkMediaItem -> {
        Uri.parse(url)
    }

    is VideoPlayerMediaItem.StorageMediaItem -> {
        val dataSpec = DataSpec(storageUri)
        val fileDataSource = FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: FileDataSourceException) {
            e.printStackTrace()
        }

        fileDataSource.uri ?: Uri.EMPTY
    }
}
