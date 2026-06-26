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

/**
 * Cross-platform top app bar.
 *
 * [navController] is intentionally typed as `Any?` to avoid leaking the Android-only
 * `androidx.navigation.NavHostController` into commonMain. The androidMain actual
 * casts it and calls `navigateUp()`; the iOS actual is a no-op.
 *
 * Pass `null` to disable the default `NavigationBackIcon` (or use a custom [navigationIcon]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun PTopAppBar(
    navController: Any? = null,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String = "",
    containerColor: Color? = null,
    subtitleColor: Color? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
)
