package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.page.apps.AppsPage

@Composable
fun PageContentApps(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    AppsPage(
        navController = navController,
        onOpenDrawer = onOpenDrawer,
    )
}
