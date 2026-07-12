package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.FeedsViewModel

@Composable
expect fun FeedsPageEffects(feedsVM: FeedsViewModel)
