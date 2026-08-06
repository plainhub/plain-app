package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class PhoneGeo(val province: String, val city: String, val zipCode: String, val areaCode: String, val isp: Int)
