package com.ismartcoding.plain.db

interface IData {
    var id: String
}

data class IDData(override var id: String) : IData

interface IMedia {
    val path: String
    val duration: Long
    val title: String
}
