package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.theme.canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PScaffold(
    navController: NavHostController,
    containerColor: Color = MaterialTheme.colorScheme.canvas(),
    navigationIcon: (@Composable () -> Unit)? = {
        NavigationBackIcon(navController)
    },
    topBarTitle: String = "",
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    Scaffold(
        containerColor = containerColor,
        topBar = {
            if (navigationIcon != null || actions != null) {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = { navigationIcon?.invoke() },
                    actions = { actions?.invoke(this) },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                )
            }
        },
        content = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Spacer(modifier = Modifier.height(it.calculateTopPadding()))
                content()
            }
        },
        bottomBar = { bottomBar?.invoke() },
        floatingActionButton = { floatingActionButton?.invoke() },
    )
}
