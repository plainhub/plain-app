package com.ismartcoding.plain.features.media

import com.ismartcoding.lib.upnp.UPnPDevice
import com.ismartcoding.plain.data.IMedia

object CastPlayer {
    var currentDevice: UPnPDevice? = null
    var items: List<IMedia>? = null
    var currentUri: String = ""
    var sid: String = ""
}
