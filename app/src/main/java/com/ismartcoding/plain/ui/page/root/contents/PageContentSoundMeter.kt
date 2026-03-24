package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.page.tools.SoundMeterPage

@Composable
fun PageContentSoundMeter(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    SoundMeterPage(
        navController = navController,
        onOpenDrawer = onOpenDrawer,
    )
}
