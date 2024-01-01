package com.ismartcoding.plain.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.audio.AudiosDialog
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.file.FilesDialog
import com.ismartcoding.plain.ui.image.ImagesDialog
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.video.VideosDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemStorage(
    navController: NavHostController,
    itemWidth: Dp,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_storage),
        )
        HomeItemFlow {
            GridItem(
                icon = Icons.Outlined.Image,
                stringResource(id = R.string.images),
                modifier = Modifier.width(itemWidth),
            ) {
                ImagesDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.AudioFile,
                stringResource(id = R.string.audios),
                modifier = Modifier.width(itemWidth),
            ) {
                AudiosDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.VideoFile,
                stringResource(id = R.string.videos),
                modifier = Modifier.width(itemWidth),
            ) {
                VideosDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.FilePresent,
                stringResource(id = R.string.files),
                modifier = Modifier.width(itemWidth),
            ) {
                FilesDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.Apps,
                stringResource(id = R.string.apps),
                modifier = Modifier.width(itemWidth),
            ) {
                navController.navigate(RouteName.APPS)
            }
        }
    }
}
