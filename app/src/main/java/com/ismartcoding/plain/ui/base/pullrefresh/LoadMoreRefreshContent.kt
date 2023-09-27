package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun RefreshLayoutState.LoadMoreRefreshContent(isLoadFinish: Boolean = false) {
    val rotate =
        if (isLoadFinish || getRefreshContentOffset() == 0f) {
            0f
        } else {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
            ).value
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(30.dp),
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
