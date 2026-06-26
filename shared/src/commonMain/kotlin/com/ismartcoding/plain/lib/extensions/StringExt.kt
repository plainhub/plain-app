package com.ismartcoding.plain.lib.extensions

fun String.getFilenameExtension() = substring(lastIndexOf(".") + 1).lowercase()
