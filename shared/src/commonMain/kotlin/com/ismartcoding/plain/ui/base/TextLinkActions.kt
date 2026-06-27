package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*

import androidx.compose.ui.text.AnnotatedString
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.ui.helpers.DialogHelper

fun AnnotatedString.urlAt(
    position: Int,
): Boolean {
    val annotations = getStringAnnotations(position, position)
    annotations.forEach {
        when (it.tag) {
            "URL" -> {
                try {
                    launchUrl(it.item)
                } catch (_: Exception) {
                    DialogHelper.showMessage(Res.string.no_browser_error)
                }
                return true
            }

            "EMAIL" -> {
                try {
                    launchUrl("mailto:${it.item}")
                } catch (_: Exception) {
                    DialogHelper.showMessage(Res.string.not_supported_error)
                }
                return true
            }

            "PHONE" -> {
                try {
                    launchUrl("tel:${it.item}")
                } catch (_: Exception) {
                    DialogHelper.showMessage(Res.string.not_supported_error)
                }
                return true
            }
        }
    }

    return false
}