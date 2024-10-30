package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBar(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    navigationIcon: (@Composable () -> Unit)? = {
        NavigationBackIcon { navController.popBackStack() }
    },
    title: String,
    subtitle: String = "",
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            if (subtitle.isEmpty()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        navigationIcon = { navigationIcon?.invoke() },
        actions = { actions?.invoke(this) },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
        scrollBehavior = scrollBehavior,
    )
}