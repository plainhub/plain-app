package com.ismartcoding.plain.ui.home.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.audio.AudiosDialog
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.file.FilesDialog
import com.ismartcoding.plain.ui.image.ImagesDialog
import com.ismartcoding.plain.ui.video.VideosDialog

@Composable
fun HomeItemStorage() {
    Column {
        Subtitle(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.home_item_title_storage)
        )
        PListItem(
            title = stringResource(R.string.images),
            showMore = true,
            onClick = {
                ImagesDialog().show()
            },
        )
        PListItem(
            title = stringResource(R.string.audios),
            showMore = true,
            onClick = {
                AudiosDialog().show()
            },
        )
        PListItem(
            title = stringResource(R.string.videos),
            showMore = true,
            onClick = {
                VideosDialog().show()
            },
        )
        PListItem(
            title = stringResource(R.string.files),
            showMore = true,
            onClick = {
                FilesDialog().show()
            },
        )
    }
}
