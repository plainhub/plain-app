package com.ismartcoding.plain.web

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

val deviceIP4s = mutableStateOf(emptyList<String>())

internal fun setDeviceIP4s(value: List<String>) {
    deviceIP4s.value = value
}
