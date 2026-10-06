package com.ismartcoding.plain.httpserver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the stored-XSS mitigation for user files served through the /fs
 * and DLNA families: document types a browser executes scripts in when
 * navigated to (SVG/HTML/XHTML) must go out with
 * `Content-Security-Policy: sandbox` — an opaque origin, so scripts and forms
 * are dead while the visual preview still renders — plus
 * `X-Content-Type-Options: nosniff`. Inert types get nosniff alone, so normal
 * media embedding is untouched.
 *
 * Asserted against [securityHeadersFor], the single rule both response paths
 * run, so breaking either one fails here.
 */
class FileResponseSecurityHeadersTest {

    @Test
    fun svg_getsSandboxCsp_andNosniff() {
        val headers = securityHeadersFor("image/svg+xml")
        assertEquals("sandbox", headers["Content-Security-Policy"], "SVG is a scriptable document")
        assertEquals("nosniff", headers["X-Content-Type-Options"])
    }

    @Test
    fun html_getsSandboxCsp_andNosniff() {
        val headers = securityHeadersFor("text/html")
        assertEquals("sandbox", headers["Content-Security-Policy"])
        assertEquals("nosniff", headers["X-Content-Type-Options"])
    }

    @Test
    fun xhtmlAndXml_getsSandboxCsp() {
        assertEquals("sandbox", securityHeadersFor("application/xhtml+xml")["Content-Security-Policy"])
        assertEquals("sandbox", securityHeadersFor("application/xml")["Content-Security-Policy"])
        assertEquals("sandbox", securityHeadersFor("image/svg+xml; charset=utf-8")["Content-Security-Policy"], "parameters must not hide the type")
    }

    @Test
    fun inertBinary_getsNosniff_butNoCsp() {
        val headers = securityHeadersFor("application/octet-stream")
        assertEquals("nosniff", headers["X-Content-Type-Options"])
        assertFalse(headers.containsKey("Content-Security-Policy"), "CSP sandbox must not be attached to inert media types")
    }

    @Test
    fun ordinaryMedia_getsNosniff_butNoCsp() {
        for (mime in listOf("video/mp4", "audio/mpeg", "image/jpeg", "image/png", "application/pdf", "text/plain")) {
            val headers = securityHeadersFor(mime)
            assertEquals("nosniff", headers["X-Content-Type-Options"], "$mime must stay embeddable")
            assertFalse(headers.containsKey("Content-Security-Policy"), "$mime must not be sandboxed")
        }
    }

    @Test
    fun aMissingContentType_isStillNosniffed() {
        val headers = securityHeadersFor(null)
        assertEquals("nosniff", headers["X-Content-Type-Options"], "an unknown type must not be served sniffable")
        assertFalse(headers.containsKey("Content-Security-Policy"))
        assertTrue(securityHeadersFor("")["X-Content-Type-Options"] == "nosniff")
    }
}