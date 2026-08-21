package com.ismartcoding.plain.data

import com.ismartcoding.plain.db.IData

data class DMediaBucket(override var id: String, val name: String, var itemCount: Int, var size: Long, val topItems: MutableList<String>) : IData
