package com.ismartcoding.plain.ui.home.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.feed.FeedEntriesDialog
import com.ismartcoding.plain.ui.note.NotesDialog

@Composable
fun HomeItemWork() {
    Column {
        Subtitle(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.home_item_title_work)
        )
        PListItem(
            title = stringResource(R.string.notes),
            showMore = true,
            onClick = {
                NotesDialog().show()
            },
        )
        PListItem(
            title = stringResource(R.string.feeds),
            showMore = true,
            onClick = {
                FeedEntriesDialog().show()
            },
        )
    }
}
