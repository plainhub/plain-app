package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource

import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.srl_header_failed as ui_string_srl_header_failed
import com.ismartcoding.plain.ui.resources.srl_header_finish as ui_string_srl_header_finish
import com.ismartcoding.plain.ui.resources.srl_header_last_refresh_days as ui_string_srl_header_last_refresh_days
import com.ismartcoding.plain.ui.resources.srl_header_last_refresh_hours as ui_string_srl_header_last_refresh_hours
import com.ismartcoding.plain.ui.resources.srl_header_last_refresh_just_now as ui_string_srl_header_last_refresh_just_now
import com.ismartcoding.plain.ui.resources.srl_header_last_refresh_minutes as ui_string_srl_header_last_refresh_minutes
import com.ismartcoding.plain.ui.resources.srl_header_refreshing as ui_string_srl_header_refreshing
import com.ismartcoding.plain.ui.resources.srl_header_pulling as ui_string_srl_header_pulling
import com.ismartcoding.plain.ui.resources.srl_header_release as ui_string_srl_header_release

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun RefreshLayoutState.PullToRefreshContent(
    createText: @Composable (RefreshContentState) -> String = {
        when (it) {
            RefreshContentState.Failed -> stringResource(UiRes.string.ui_string_srl_header_failed)
            RefreshContentState.Finished -> stringResource(UiRes.string.ui_string_srl_header_finish)
            RefreshContentState.Refreshing -> stringResource(UiRes.string.ui_string_srl_header_refreshing)
            RefreshContentState.Dragging -> {
                if (abs(getRefreshContentOffset()) < getRefreshContentThreshold()) {
                    stringResource(UiRes.string.ui_string_srl_header_pulling)
                } else {
                    stringResource(UiRes.string.ui_string_srl_header_release)
                }
            }
        }
    },
    /**
     * 上次成功刷新的第二行。传 `null` 关掉（自己接管刷新头时用）。
     *
     * 默认开启且「没刷过时不显示」——空着比显示「上次刷新：无」干净。
     */
    showLastRefresh: Boolean = true,
) {
    val refreshContentState by remember { getRefreshContentState() }
    // 订阅而不是在组合里现算：刷新成功后这行要自己变，不依赖调用方重组。
    val lastRefresh by remember { getLastRefreshTime() }
    val elapsed = if (showLastRefresh) millisSinceLastRefresh() else null

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = createText(refreshContentState),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showLastRefresh) {
            Text(
                // Keep the header height stable when the first refresh succeeds.
                text = elapsed?.let { lastRefreshLabel(it) }.orEmpty(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** 把毫秒差变成「上次刷新：3 分钟前」这样的一行。 */
@Composable
private fun lastRefreshLabel(elapsedMillis: Long): String {
    val (bucket, value) = lastRefreshBucket(elapsedMillis)
    return when (bucket) {
        LastRefreshBucket.JustNow ->
            stringResource(UiRes.string.ui_string_srl_header_last_refresh_just_now)

        LastRefreshBucket.Minutes ->
            stringResource(UiRes.string.ui_string_srl_header_last_refresh_minutes, value)

        LastRefreshBucket.Hours ->
            stringResource(UiRes.string.ui_string_srl_header_last_refresh_hours, value)

        LastRefreshBucket.Days ->
            stringResource(UiRes.string.ui_string_srl_header_last_refresh_days, value)
    }
}
