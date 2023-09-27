package com.ismartcoding.plain.data

interface IData {
    var id: String
}

data class IDData(override var id: String) : IData

interface IMedia {
    val path: String
    val duration: Long
}
