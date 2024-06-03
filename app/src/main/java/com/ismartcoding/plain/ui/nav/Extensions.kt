package com.ismartcoding.plain.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavGraphBuilder.routeDetail(routeName: RouteName, action: @Composable (NavBackStackEntry, String) -> Unit) {
    composable(
        "${routeName.name}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        val id = it.arguments?.getString("id") ?: ""
        action(it, id)
    }
}

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavHostController): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(parentEntry)
}