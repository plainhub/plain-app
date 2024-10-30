package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconTextButton
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigateFiles

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeFeatures(
    navController: NavHostController,
    itemWidth: Dp,
) {
    PCard {
        HomeItemFlow {
            PIconTextButton(
                icon = painterResource(R.drawable.bot),
                stringResource(id = R.string.send_to_pc),
                modifier = Modifier
                    .width(itemWidth)
                    .clickable {
                        navController.navigate(Routing.Chat)

                    })
            PIconTextButton(
                icon = painterResource(R.drawable.folder),
                stringResource(id = R.string.files),
                modifier = Modifier
                    .width(itemWidth)
                    .clickable {
                        navController.navigateFiles()
                    },
            )
            PIconTextButton(
                icon = painterResource(R.drawable.file_text),
                stringResource(id = R.string.docs),
                modifier = Modifier
                    .width(itemWidth)
                    .clickable {
                        navController.navigate(Routing.Docs)
                    },
            )
            if (AppFeatureType.APPS.has()) {
                PIconTextButton(
                    icon = painterResource(R.drawable.layout_grid),
                    stringResource(id = R.string.apps),
                    modifier = Modifier
                        .width(itemWidth)
                        .clickable {
                            navController.navigate(Routing.Apps)
                        },
                )
            }
            PIconTextButton(
                icon = painterResource(R.drawable.notebook_pen),
                stringResource(id = R.string.notes),
                modifier = Modifier
                    .width(itemWidth)
                    .clickable {
                        navController.navigate(Routing.Notes)
                    },
            )
            PIconTextButton(
                icon = painterResource(R.drawable.rss),
                stringResource(id = R.string.feeds),
                modifier = Modifier
                    .width(itemWidth)
                    .clickable {
                        navController.navigate(Routing.FeedEntries(""))
                    },
            )
//            PIconTextButton(
//                icon = painterResource(R.drawable.audio_lines),
//                stringResource(id = R.string.sound_meter),
//                modifier = Modifier.width(itemWidth),
//            ) {
//                navController.navigate(Routing.SoundMeter)
//            }
        }
    }
}
