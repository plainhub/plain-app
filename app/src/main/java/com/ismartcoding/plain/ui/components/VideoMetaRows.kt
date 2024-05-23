package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.extensions.formatBitrate
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DVideoMeta
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.ui.base.PListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun VideoMetaRows(path: String) {

    var meta by remember {
        mutableStateOf<DVideoMeta?>(null)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            meta = VideoHelper.getMeta(path)
        }
    }

    meta?.let { mt ->
        if (mt.takenAt != null) {
            PListItem(title = stringResource(id = R.string.taken_at), value = mt.takenAt.formatDateTime())
        }
        if (mt.title.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.title), value = mt.title)
        }
        PListItem(title = stringResource(id = R.string.duration), value = mt.duration.formatDuration())
        PListItem(title = stringResource(id = R.string.bitrate), value =  mt.bitrate.formatBitrate())
    }
}