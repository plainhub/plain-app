package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Platform-specific route composables that create the appropriate ViewModels
 * and delegate to [com.ismartcoding.plain.ui.page.chat.ChatListPage].
 *
 * On Android the real [com.ismartcoding.plain.ui.models.MainViewModel] etc.
 * are created via `viewModel()`. On iOS the stub implementations
 * ([com.ismartcoding.plain.ui.models.IosMainViewModel] etc.) are used.
 */
@Composable
expect fun ChatListPageRoute(navController: NavHostController)

/**
 * Platform-specific route composable that creates the appropriate ViewModels
 * and delegates to [com.ismartcoding.plain.ui.page.chat.ChatPage].
 */
@Composable
expect fun ChatPageRoute(navController: NavHostController, id: String)
