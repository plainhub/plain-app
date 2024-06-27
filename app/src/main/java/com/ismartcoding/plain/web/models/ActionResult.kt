package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.enums.DataType

data class ActionResult(val type: DataType, val query: String)