package com.ismartcoding.plain.tests

import com.ismartcoding.plain.ui.web.ShizukuStep
import com.ismartcoding.plain.ui.web.currentShizukuStep
import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuSetupTest {
    private val unexpected = { error("Queried beyond the first applicable setup step") }

    @Test fun missingManagerOffersInstallation() {
        assertEquals(ShizukuStep.Install, currentShizukuStep({ false }, { false }, unexpected, unexpected))
    }

    @Test fun stoppedServiceOffersStartupWithoutQueryingPermission() {
        assertEquals(ShizukuStep.Start, currentShizukuStep({ false }, { true }, { false }, unexpected))
    }

    @Test fun runningServiceOffersAuthorization() {
        assertEquals(ShizukuStep.Authorize, currentShizukuStep({ false }, { true }, { true }, { false }))
    }

    @Test fun blockedPermissionRequestOpensManager() {
        assertEquals(ShizukuStep.Manage, currentShizukuStep({ false }, { true }, { true }, { true }))
    }

    @Test fun grantCompletesSetup() {
        assertEquals(ShizukuStep.Ready, currentShizukuStep({ true }, unexpected, unexpected, unexpected))
    }
}
