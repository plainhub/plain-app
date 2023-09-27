package com.ismartcoding.plain.ui.extensions

import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.page.RouteName

fun NavHostController.navigate(route: RouteName) {
    navigate(route.name) {
        launchSingleTop = true
    }
}
