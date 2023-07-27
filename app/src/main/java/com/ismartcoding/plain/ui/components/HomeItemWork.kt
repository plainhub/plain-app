package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.feed.FeedEntriesDialog
import com.ismartcoding.plain.ui.note.NotesDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemWork() {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_work)
        )
        HomeItemFlow {
            GridItem(icon = Icons.Outlined.Notes, stringResource(id = R.string.notes), modifier = Modifier.weight(1f)) {
                NotesDialog().show()
            }
            GridItem(icon = Icons.Outlined.RssFeed, stringResource(id = R.string.feeds), modifier = Modifier.weight(1f)) {
                FeedEntriesDialog().show()
            }
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}
