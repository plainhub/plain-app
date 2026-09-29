package com.ismartcoding.plain.ui.base.pullrefresh

import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.loading as ui_string_loading
import com.ismartcoding.plain.ui.resources.srl_header_failed as ui_string_srl_header_failed
import com.ismartcoding.plain.ui.resources.srl_header_finish as ui_string_srl_header_finish
import com.ismartcoding.plain.ui.resources.srl_header_refreshing as ui_string_srl_header_refreshing
import com.ismartcoding.plain.ui.resources.srl_header_pulling as ui_string_srl_header_pulling
import com.ismartcoding.plain.ui.resources.srl_header_release as ui_string_srl_header_release

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadMoreRefreshContent(isLoadFinish: Boolean = false) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (!isLoadFinish) {
            Text(
                text = stringResource(UiRes.string.ui_string_loading),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
