package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.R

@Composable
fun LoadMoreRefreshContent(isLoadFinish: Boolean = false) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                if (isLoadFinish) {
                    stringResource(id = R.string.load_no_more)
                } else {
                    stringResource(id = R.string.loading)
                },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
