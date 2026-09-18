package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

/**
 * Offline phone number metadata resolved via libphonenumber.
 * All fields empty when unknown; [carrier] and [description] are localized
 * by the device locale, [country] is an ISO region code and [numberType]
 * is a libphonenumber PhoneNumberType name (e.g. "MOBILE", "TOLL_FREE").
 */
@GraphQLType
data class PhoneGeo(val country: String, val numberType: String, val carrier: String, val description: String)
