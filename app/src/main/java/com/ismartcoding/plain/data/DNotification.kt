package com.ismartcoding.plain.data

import kotlinx.datetime.Instant

data class DNotification(
    val id: String,
    val onlyOnce: Boolean,
    val isClearable: Boolean,
    val appName: String,
    val time: Instant,
    val silent: Boolean,
    val title: String,
    val text: String,
    val actions: List<String>
)