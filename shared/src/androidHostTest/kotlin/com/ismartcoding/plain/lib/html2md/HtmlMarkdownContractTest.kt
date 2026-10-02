package com.ismartcoding.plain.lib.html2md

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlMarkdownContractTest {
    @Test
    fun exactMarkdownBehavior() {
        val fixture = File("apitest/html_to_markdown.json")
        val cases = Json.parseToJsonElement(fixture.readText()).jsonArray
        assertTrue(cases.size >= 212)
        val converter = MDConverter()
        for (case in cases) {
            val values = case.jsonObject
            assertEquals(
                values.getValue("markdown").jsonPrimitive.content,
                converter.convert(values.getValue("html").jsonPrimitive.content),
                values.getValue("name").jsonPrimitive.content,
            )
        }
        val rustFixture = File("../../plain-desktop/plain-rs/testdata/html_to_markdown.json")
        if (rustFixture.exists()) {
            assertEquals(fixture.readText(), rustFixture.readText(), "Kotlin and Rust fixtures must stay identical")
        }
    }
}
