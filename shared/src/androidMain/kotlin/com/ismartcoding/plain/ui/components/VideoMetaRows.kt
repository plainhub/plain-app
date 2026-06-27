package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.extensions.formatBitrate
import com.ismartcoding.plain.lib.extensions.formatDuration
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
        mt.takenAt?.let { takenAt ->
            PListItem(title = stringResource(Res.string.taken_at), value = takenAt.formatDateTime())
        }
        if (mt.title.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.title), value = mt.title)
        }
        PListItem(title = stringResource(Res.string.duration), value = mt.duration.formatDuration())
        PListItem(title = stringResource(Res.string.bitrate), value =  mt.bitrate.formatBitrate())
    }
}