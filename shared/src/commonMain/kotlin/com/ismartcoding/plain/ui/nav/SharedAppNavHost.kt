package com.ismartcoding.plain.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ismartcoding.plain.platform.ChatListPageRoute
import com.ismartcoding.plain.platform.ChatPageRoute

@Composable
fun SharedAppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routing.ChatList,
    ) {
        composable<Routing.Home> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {}
        }
        composable<Routing.ChatList> {
            ChatListPageRoute(navController)
        }
        composable<Routing.Chat> { backStackEntry ->
            val r = backStackEntry.toRoute<Routing.Chat>()
            ChatPageRoute(navController, r.id)
        }
    }
}
