package com.ismartcoding.plain.ui.nav

import android.net.Uri
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.enums.TextFileType


// https://stackoverflow.com/questions/67121433/how-to-pass-object-in-navigation-in-jetpack-compose
fun NavHostController.navigateText(title: String, content: String, language: String) {
    currentBackStackEntry?.savedStateHandle?.set("title", title)
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    currentBackStackEntry?.savedStateHandle?.set("language", language)
    navigate(Routing.Text) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateChatEditText(id: String, content: String) {
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    navigate(Routing.ChatEditText(id)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateChatText(content: String) {
    currentBackStackEntry?.savedStateHandle?.set("content", content)
    navigate(Routing.ChatText) {
        launchSingleTop = true
    }
}

fun NavHostController.navigatePdf(uri: Uri) {
    currentBackStackEntry?.savedStateHandle?.set("uri", uri)
    navigate(Routing.PdfViewer) {
        launchSingleTop = true
    }
}


fun NavHostController.navigateOtherFile(path: String) {
    currentBackStackEntry?.savedStateHandle?.set("path", path)
    navigate(Routing.OtherFile) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateTextFile(path: String, title: String = "", mediaId: String = "", type: TextFileType = TextFileType.DEFAULT) {
    currentBackStackEntry?.savedStateHandle?.set("path", path)
    currentBackStackEntry?.savedStateHandle?.set("title", title)
    currentBackStackEntry?.savedStateHandle?.set("mediaId", mediaId)
    currentBackStackEntry?.savedStateHandle?.set("type", type.name)
    navigate(Routing.TextFile) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateFiles(fileType: FilesType = FilesType.INTERNAL_STORAGE) {
    navigate(Routing.Files(fileType.ordinal)) {
        launchSingleTop = true
    }
}
