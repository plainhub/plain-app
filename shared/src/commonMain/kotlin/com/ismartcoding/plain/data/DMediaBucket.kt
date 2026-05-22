package com.ismartcoding.plain.data

data class DMediaBucket(override var id: String, val name: String, var itemCount: Int, var size: Long, val topItems: MutableList<String>) : IData
