package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.platform.NetworkType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedFetcherTest {

    @Test
    fun manual_refresh_never_skipped_regardless_of_network() {
        // autoRefresh == false must always proceed, even if onlyWifi is on
        // and the device is on cellular.
        listOf(NetworkType.WIFI, NetworkType.CELLULAR, NetworkType.ETHERNET, NetworkType.NONE)
            .forEach { network ->
                assertFalse(
                    shouldSkipAutoRefresh(autoRefresh = false, onlyWifi = true, networkType = network),
                    "manual refresh should not skip on $network",
                )
            }
    }

    @Test
    fun auto_refresh_skips_when_only_wifi_and_not_on_wifi() {
        // onlyWifi on + not wifi → skip (this is the intended behavior)
        listOf(NetworkType.CELLULAR, NetworkType.ETHERNET, NetworkType.NONE)
            .forEach { network ->
                assertTrue(
                    shouldSkipAutoRefresh(autoRefresh = true, onlyWifi = true, networkType = network),
                    "auto refresh with onlyWifi should skip on $network",
                )
            }
    }

    @Test
    fun auto_refresh_proceeds_when_only_wifi_and_on_wifi() {
        // Regression guard: the previous implementation skipped on wifi,
        // which inverted the "only refresh on wifi" semantics.
        assertFalse(
            shouldSkipAutoRefresh(autoRefresh = true, onlyWifi = true, networkType = NetworkType.WIFI),
            "auto refresh with onlyWifi must proceed on wifi",
        )
    }

    @Test
    fun auto_refresh_proceeds_when_only_wifi_disabled() {
        // onlyWifi off → never skip, regardless of network
        listOf(NetworkType.WIFI, NetworkType.CELLULAR, NetworkType.ETHERNET, NetworkType.NONE)
            .forEach { network ->
                assertFalse(
                    shouldSkipAutoRefresh(autoRefresh = true, onlyWifi = false, networkType = network),
                    "auto refresh without onlyWifi should not skip on $network",
                )
            }
    }
}
