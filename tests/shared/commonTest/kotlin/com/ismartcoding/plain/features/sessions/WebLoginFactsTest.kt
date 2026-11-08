package com.ismartcoding.plain.features.sessions

import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WebLoginFactsTest {
    private val metadata = """"appName":"Fixture","appVersion":"1","osName":"Fixture","osVersion":"1","isMobile":false"""

    @Test
    fun passwordlessApprovalKeepsTheServerVerificationCodeWithoutRequiringAPassword() {
        val facts = JsonHelper.jsonDecode<WebLoginRequestFacts>("""{"clientId":"browser","requestId":"request","clientIp":"127.0.0.1","request":{$metadata,"verificationCode":"0123 4567","loginContext":"context"}}""")
        assertEquals("", facts.request.password)
        assertEquals("0123 4567", facts.request.verificationCode)
        assertEquals("request", facts.requestId)
    }

    @Test
    fun completedLoginFactsDoNotNeedTheRemovedPasswordField() {
        val facts = JsonHelper.jsonDecode<WebLoginCompletedFacts>("""{"request":{$metadata},"chatPaired":false,"clientIp":"127.0.0.1"}""")
        assertEquals("", facts.request.password)
        assertEquals("", facts.request.verificationCode)
        assertFalse(facts.chatPaired)
    }

    @Test
    fun closingFactsIdentifyOnlyTheirOwnPendingRequest() {
        val facts = JsonHelper.jsonDecode<WebLoginClosedFacts>("""{"requestId":"request"}""")
        assertEquals("request", facts.requestId)
        assertEquals("""{"requestId":"request"}""", JsonHelper.jsonEncodeToElement(facts).toString())
    }
}
