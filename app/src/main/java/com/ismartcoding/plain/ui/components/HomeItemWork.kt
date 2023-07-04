package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GridItem(icon = Icons.Outlined.Notes, stringResource(id = R.string.notes), modifier = Modifier.weight(1f)) {
                NotesDialog().show()
            }
            Spacer(modifier = Modifier.width(8.dp))
            GridItem(icon = Icons.Outlined.RssFeed, stringResource(id = R.string.feeds), modifier = Modifier.weight(1f)) {
                FeedEntriesDialog().show()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}
