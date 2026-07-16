package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DSim

/**
 * Returns the list of active SIM subscriptions on the device.
 * Returns an empty list on platforms without telephony support or when the
 * READ_PHONE_STATE permission is not granted.
 */
expect fun getSims(): List<DSim>
