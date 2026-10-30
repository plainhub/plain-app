package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ismartcoding.plain.ui.app.MainViewModel

@Composable
internal fun MainRequestNavigation(navController: NavHostController) {
    val loginRequest by MainViewModel.pendingLoginRequest
    val pairingRequest by MainViewModel.pendingPairingRequest
    val channelInvite by MainViewModel.pendingChannelInvite
    com.ismartcoding.plain.ui.web.WebAccessRequestDialog()

    LaunchedEffect(loginRequest) {
        if (loginRequest == null) {
            if (navController.currentBackStackEntry?.destination?.hasRoute<Routing.LoginRequest>() == true) {
                navController.popBackStack<Routing.LoginRequest>(inclusive = true)
            }
        } else if (navController.currentBackStackEntry?.destination?.hasRoute<Routing.LoginRequest>() != true) {
            navController.navigate(Routing.LoginRequest)
        }
    }

    LaunchedEffect(pairingRequest) {
        if (pairingRequest == null) {
            if (navController.currentBackStackEntry?.destination?.hasRoute<Routing.PairingRequest>() == true) {
                navController.popBackStack<Routing.PairingRequest>(inclusive = true)
            }
        } else if (navController.currentBackStackEntry?.destination?.hasRoute<Routing.PairingRequest>() != true) {
            navController.navigate(Routing.PairingRequest)
        }
    }

    LaunchedEffect(channelInvite) {
        val invite = channelInvite
        val current = navController.currentBackStackEntry
        if (invite == null) {
            if (current?.destination?.hasRoute<Routing.ChannelInviteRequest>() == true) {
                navController.popBackStack<Routing.ChannelInviteRequest>(inclusive = true)
            }
        } else if (current?.destination?.hasRoute<Routing.ChannelInviteRequest>() != true ||
            current.toRoute<Routing.ChannelInviteRequest>() != invite) {
            navController.navigate(invite) { launchSingleTop = true }
        }
    }
}
