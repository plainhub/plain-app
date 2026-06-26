package com.ismartcoding.plain.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ismartcoding.plain.shared.home.PlainHomeScreen

@Composable
fun SharedAppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routing.Home,
    ) {
        composable<Routing.Home> {
            PlainHomeScreen()
        }
        // Phase 6.1+ 完成后注册其它路由（Settings/Notes/Tags/Feeds/Chat/...）
    }
}
