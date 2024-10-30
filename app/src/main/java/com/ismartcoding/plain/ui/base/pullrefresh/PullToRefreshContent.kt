package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import kotlin.math.abs

@Composable
fun RefreshLayoutState.PullToRefreshContent(
    createText: @Composable (RefreshContentState) -> String = {
        when (it) {
            RefreshContentState.Failed -> stringResource(id = R.string.srl_header_failed)
            RefreshContentState.Finished -> stringResource(id = R.string.srl_header_finish)
            RefreshContentState.Refreshing -> stringResource(id = R.string.srl_header_refreshing)
            RefreshContentState.Dragging -> {
                if (abs(getRefreshContentOffset()) < getRefreshContentThreshold()) {
                    stringResource(id = R.string.srl_header_pulling)
                } else {
                    stringResource(id = R.string.srl_header_release)
                }
            }
        }
    }
) {
    val refreshContentState by remember {
        getRefreshContentState()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = createText(refreshContentState),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondaryTextColor,
        )
    }
}
