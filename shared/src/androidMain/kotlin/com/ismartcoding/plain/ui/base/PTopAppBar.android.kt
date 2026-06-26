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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PTopAppBar(
    navController: Any?,
    modifier: Modifier,
    navigationIcon: (@Composable () -> Unit)?,
    title: String,
    subtitle: String,
    containerColor: Color?,
    subtitleColor: Color?,
    actions: (@Composable RowScope.() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val topBarColor = containerColor ?: MaterialTheme.colorScheme.background
    val topBarSubtitleColor = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    val nav = navController as? NavHostController
    TopAppBar(
        title = {
            if (subtitle.isEmpty()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = topBarSubtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        navigationIcon = {
            when {
                navigationIcon != null -> navigationIcon()
                nav != null -> NavigationBackIcon(onClick = { nav.navigateUp() })
            }
        },
        actions = {
            actions?.invoke(this)
            HorizontalSpace(8.dp)
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarColor,
            scrolledContainerColor = topBarColor,
        ),
        scrollBehavior = scrollBehavior,
    )
}
