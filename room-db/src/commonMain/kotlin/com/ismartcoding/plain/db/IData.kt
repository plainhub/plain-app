package com.ismartcoding.plain.db

import com.ismartcoding.plain.ui.Identifiable

interface IData : Identifiable {
    override var id: String
}

data class IDData(override var id: String) : IData

interface IMedia {
    val path: String
    val durationMs: Long
    val title: String
}
