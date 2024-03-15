package com.ismartcoding.plain.ui.components.home

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.AppFeatureType
import com.ismartcoding.plain.ui.audio.AudiosDialog
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.feed.FeedEntriesDialog
import com.ismartcoding.plain.ui.file.FilesDialog
import com.ismartcoding.plain.ui.image.ImagesDialog
import com.ismartcoding.plain.ui.note.NotesDialog
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.video.VideosDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeFeatures(
    navController: NavHostController,
    itemWidth: Dp,
) {
    PCard {
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
            if (AppFeatureType.APPS.has()) {
                GridItem(
                    icon = Icons.Outlined.Apps,
                    stringResource(id = R.string.apps),
                    modifier = Modifier.width(itemWidth),
                ) {
                    navController.navigate(RouteName.APPS)
                }
            }
            GridItem(
                icon = Icons.AutoMirrored.Outlined.Notes,
                stringResource(id = R.string.notes),
                modifier = Modifier.width(itemWidth),
            ) {
                NotesDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.RssFeed,
                stringResource(id = R.string.feeds),
                modifier = Modifier.width(itemWidth),
            ) {
                FeedEntriesDialog().show()
            }
            GridItem(
                icon = Icons.Outlined.GraphicEq,
                stringResource(id = R.string.sound_meter),
                modifier = Modifier.width(itemWidth),
            ) {
                navController.navigate(RouteName.SOUND_METER)
            }
//                if (AppFeatureType.EXCHANGE_RATE.has()) {
//                    GridItem(
//                        icon = Icons.Outlined.CurrencyExchange,
//                        stringResource(id = R.string.exchange_rate),
//                        modifier = Modifier.width(itemWidth),
//                    ) {
//                        navController.navigate(RouteName.EXCHANGE_RATE)
//                    }
//                }
//                GridItem(
//                    icon = Icons.Outlined.Language,
//                    stringResource(id = R.string.memorize_words),
//                    modifier =
//                    Modifier
//                        .width(itemWidth),
//                ) {
//                    VocabulariesDialog().show()
//                }
//                GridItem(
//                    icon = Icons.AutoMirrored.Outlined.Message,
//                    stringResource(id = R.string.messages),
//                    modifier = Modifier.width(itemWidth),
//                ) {
//                    SmsDialog().show()
//                }
//                GridItem(
//                    icon = Icons.Outlined.Contacts,
//                    stringResource(id = R.string.contacts),
//                    modifier = Modifier.width(itemWidth),
//                ) {
//                    ContactsDialog().show()
//                }
//                GridItem(
//                    icon = Icons.Outlined.Call,
//                    stringResource(id = R.string.calls),
//                    modifier = Modifier.width(itemWidth),
//                ) {
//                    CallsDialog().show()
//                }
        }
    }
}
