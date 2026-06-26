package com.ismartcoding.plain.helpers

import java.io.File

fun String.getFilenameFromPath() = substring(lastIndexOf("/") + 1)
fun String.getParentPath() = this.substringBeforeLast("/")
fun String.relativizeWith(path: String) = this.substring(path.length)