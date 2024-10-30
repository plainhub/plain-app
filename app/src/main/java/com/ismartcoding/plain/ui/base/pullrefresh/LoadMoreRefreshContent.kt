package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.secondaryTextColor

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
                text = stringResource(id = R.string.loading),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondaryTextColor,
            )
        }
    }
}
