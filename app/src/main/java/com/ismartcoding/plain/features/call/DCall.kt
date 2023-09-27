package com.ismartcoding.plain.features.call

import com.ismartcoding.lib.phonegeo.PhoneNumberLookup
import com.ismartcoding.lib.phonegeo.algo.LookupAlgorithm
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.web.models.PhoneGeo
import kotlinx.datetime.Instant

data class DCall(
    override var id: String,
    var number: String,
    var name: String,
    var photoUri: String,
    var startedAt: Instant,
    var duration: Int,
    var type: Int,
    val accountId: String,
) : IData {
    fun getGeo(): PhoneGeo? {
        var geo: PhoneGeo? = null
        PhoneNumberLookup.instance().with(LookupAlgorithm.IMPL.BINARY_SEARCH).lookup(number)?.let {
            geo = PhoneGeo(it.geoInfo.province, it.geoInfo.city, it.geoInfo.zipCode, it.geoInfo.areaCode, it.isp.code)
        }
        return geo
    }
}
