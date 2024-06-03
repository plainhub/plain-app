package com.ismartcoding.plain.ui.nav

import android.net.Uri
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.TextFileType

fun NavHostController.navigate(route: RouteName) {
    navigate(route.name) {
        launchSingleTop = true
    }
}

// https://stackoverflow.com/questions/67121433/how-to-pass-object-in-navigation-in-jetpack-compose
fun NavHostController.navigateText(title: String, content: String, language: String) {
    currentBackStackEntry?.savedStateHandle?.set("title", title)
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    currentBackStackEntry?.savedStateHandle?.set("language", language)
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

fun NavHostController.navigateTextFile(path: String, title: String = "", mediaId: String = "", type: TextFileType = TextFileType.DEFAULT) {
    currentBackStackEntry?.savedStateHandle?.set("path", path)
    currentBackStackEntry?.savedStateHandle?.set("title", title)
    currentBackStackEntry?.savedStateHandle?.set("mediaId", mediaId)
    currentBackStackEntry?.savedStateHandle?.set("type", type.name)
    navigate(RouteName.TEXT_FILE)
}

fun NavHostController.navigateTags(dateType: DataType) {
    navigate("${RouteName.TAGS.name}?dataType=${dateType.value}")
}

fun NavHostController.navigateImages(bucketId: String = "") {
    navigate("${RouteName.IMAGES.name}?bucketId=${bucketId}")
}

fun NavHostController.navigateVideos(bucketId: String = "") {
    navigate("${RouteName.VIDEOS.name}?bucketId=${bucketId}")
}

fun NavHostController.navigateMediaFolders(dateType: DataType) {
    navigate("${RouteName.MEDIA_FOLDERS.name}?dataType=${dateType.value}")
}

fun NavHostController.navigateDetail(routeName: RouteName, id: String) {
    navigate("${routeName.name}/${id}")
}