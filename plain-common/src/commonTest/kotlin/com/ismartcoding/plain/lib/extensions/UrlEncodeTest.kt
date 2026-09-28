package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlEncodeTest {

    // ── URL decoding ───

    @Test
    fun `decodes chinese filename`() {
        val input = "%E6%8A%80%E6%9C%AF%E6%8A%A5%E5%91%8A.pdf"
        assertEquals("技术报告.pdf", input.urlDecode())
    }

    @Test
    fun `decodes plain ascii unchanged`() {
        assertEquals("report.pdf", "report.pdf".urlDecode())
    }

    @Test
    fun `decodes plus as space`() {
        assertEquals("my report.pdf", "my+report.pdf".urlDecode())
    }

    @Test
    fun `leaves invalid percent sequences untouched`() {
        assertEquals("file%zY.pdf", "file%zY.pdf".urlDecode())
    }

    @Test
    fun `decodes empty string`() {
        assertEquals("", "".urlDecode())
    }

    @Test
    fun `decodes mixed ascii and chinese`() {
        val input = "Q3_%E6%B5%8B%E8%AF%95.txt"
        assertEquals("Q3_测试.txt", input.urlDecode())
    }
}