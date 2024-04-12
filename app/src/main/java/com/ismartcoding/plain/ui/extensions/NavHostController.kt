package com.ismartcoding.plain.ui.extensions

import android.net.Uri
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.page.RouteName

fun NavHostController.navigate(route: RouteName) {
    navigate(route.name) {
        launchSingleTop = true
    }
}

// https://stackoverflow.com/questions/67121433/how-to-pass-object-in-navigation-in-jetpack-compose
fun NavHostController.navigateText(title: String, content: String) {
    currentBackStackEntry?.savedStateHandle?.set("title", title)
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    navigate(RouteName.TEXT)
}

fun NavHostController.navigateChatEditText(id: String, content: String) {
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    navigate("${RouteName.CHAT_EDIT_TEXT.name}/${id}") {
        launchSingleTop = true
    }
}

fun NavHostController.navigateChatText(content: String) {
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    navigate(RouteName.CHAT_TEXT)
}

fun NavHostController.navigatePdf(uri: Uri) {
    currentBackStackEntry?.savedStateHandle?.set("uri", uri)
    navigate(RouteName.PDF_VIEWER)
}


fun NavHostController.navigateOtherFile(path: String) {
    currentBackStackEntry?.savedStateHandle?.set("path", path)
    navigate(RouteName.OTHER_FILE)
}
