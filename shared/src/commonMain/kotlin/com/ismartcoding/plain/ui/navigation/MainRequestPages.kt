package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.chat.ChannelInvitePage
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.nearby.PairingRequestPage
import com.ismartcoding.plain.ui.sessions.LoginRequestPage

internal fun NavGraphBuilder.addMainRequestPages(
    navController: NavHostController,
    channelVM: ChannelViewModel,
) {
    composableNoAnim<Routing.PairingRequest> { backStackEntry ->
        val request = MainViewModel.pendingPairingRequest.value
        if (request != null) {
            key(request) {
                PairingRequestPage(notice = request, onClose = {
                    if (MainViewModel.pendingPairingRequest.value == request) {
                        MainViewModel.pendingPairingRequest.value = null
                        if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
                    }
                })
            }
        } else {
            navController.popBackStack<Routing.PairingRequest>(inclusive = true)
        }
    }
    composableNoAnim<Routing.LoginRequest> { backStackEntry ->
        val request = MainViewModel.pendingLoginRequest.value
        if (request != null) {
            key(request.requestId) {
                LoginRequestPage(loginRequest = request, onClose = {
                    if (MainViewModel.pendingLoginRequest.value?.requestId == request.requestId) {
                        MainViewModel.pendingLoginRequest.value = null
                    }
                    if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
                })
            }
        } else {
            navController.popBackStack<Routing.LoginRequest>(inclusive = true)
        }
    }
    composableNoAnim<Routing.ChannelInviteRequest> { backStackEntry ->
        val invite = backStackEntry.toRoute<Routing.ChannelInviteRequest>()
        if (MainViewModel.pendingChannelInvite.value == null) {
            navController.popBackStack<Routing.ChannelInviteRequest>(inclusive = true)
        } else key(invite) {
            ChannelInvitePage(
                channelId = invite.channelId,
                channelName = invite.channelName,
                ownerPeerName = invite.ownerPeerName,
                channelVM = channelVM,
                onClose = {
                    if (MainViewModel.pendingChannelInvite.value == invite) MainViewModel.pendingChannelInvite.value = null
                    if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
                },
            )
        }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.composableNoAnim(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        content = content,
    )
}
