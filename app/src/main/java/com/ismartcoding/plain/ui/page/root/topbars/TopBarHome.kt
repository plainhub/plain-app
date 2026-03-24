package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.ActionButtonDrawer
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.nav.Routing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHome(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
) {
    PTopAppBar(
        navController = navController,
        navigationIcon = {
            ActionButtonDrawer(onClick = onOpenDrawer)
        },
        title = stringResource(R.string.phone_web_portal),
        actions = {
            PIconButton(
                icon = R.drawable.info,
                contentDescription = stringResource(R.string.learn_more),
                click = { navController.navigate(Routing.WebLearnMore) },
            )
        },
    )
}