package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import kotlinx.coroutines.runBlocking
import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlin.test.assertEquals

class PairingProjectionTest {
    @AfterTest
    fun clear() {
        NearbyViewModel.pairingStates.clear()
        NearbyViewModel.itemStatus.clear()
    }

    private fun state(generation: String, phase: String) = JsonHelper.jsonDecode<PairingState>("""{"deviceId":"peer","generation":"$generation","mode":"CHAT","phase":"$phase","error":null}""")

    @Test
    fun cachedGenerationsProjectWithoutANativeBridgeAndOldEventsCannotClearNewWork() = runBlocking {
        PairingProjection.applyState(state("first", "STARTING"))
        assertEquals(NearbyItemStatus.STARTING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.applyState(state("first", "CANCELLED"))
        PairingProjection.applyState(state("second", "STARTING"))
        assertEquals(NearbyItemStatus.STARTING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.applyState(state("second", "SUCCEEDED"))
        assertEquals(NearbyItemStatus.PAIRED, NearbyViewModel.itemStatus["peer"])
    }

    @Test
    fun unknownPhasesAreRejectedAndOlderSnapshotsCannotRegressKnownProgress() {
        PairingProjection.applyState(state("first", "COMMITTING"))
        PairingProjection.applyState(state("first", "STARTING"))
        assertEquals(PairingPhase.COMMITTING, NearbyViewModel.pairingStates["first"]?.phase)
        assertFailsWith<SerializationException> {
            state("other", "FUTURE_PHASE")
        }
        assertEquals(null, NearbyViewModel.pairingStates["other"])
        assertEquals(NearbyItemStatus.PAIRING, NearbyViewModel.itemStatus["peer"])
    }
    @Test
    fun anOlderSnapshotCannotRemoveAnAttemptStartedAfterTheRead() {
        PairingProjection.applyState(state("first", "STARTING"))
        val before = NearbyViewModel.pairingStates.toMap()
        PairingProjection.applyState(state("first", "CANCELLED"))
        PairingProjection.applyState(state("second", "PASSWORD_REQUIRED"))
        PairingProjection.applySnapshot(before, listOf(state("first", "STARTING")))
        assertEquals(PairingPhase.PASSWORD_REQUIRED, NearbyViewModel.pairingStates["second"]?.phase)
        assertEquals(NearbyItemStatus.PAIRING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.applySnapshot(NearbyViewModel.pairingStates.toMap(), emptyList())
        assertEquals(null, NearbyViewModel.itemStatus["peer"])
    }

}
