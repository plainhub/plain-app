package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.page.docs.DocsPage

@Composable
fun PageContentDocs(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    DocsPage(
        navController = navController,
        onOpenDrawer = onOpenDrawer,
    )
}
