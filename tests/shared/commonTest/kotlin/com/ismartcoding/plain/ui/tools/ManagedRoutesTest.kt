package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.ui.navigation.Routing
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlin.test.Test
import kotlin.test.assertEquals

class ManagedRoutesTest {
    @Test
    fun management_routes_use_portable_enum_names_for_native_navigation() {
        val content = Routing.ManagedContent.serializer().descriptor
        val document = Routing.ManagedDocument.serializer().descriptor
        assertEquals(PrimitiveKind.STRING, content.getElementDescriptor(content.getElementIndex("feature")).kind)
        assertEquals(PrimitiveKind.STRING, document.getElementDescriptor(document.getElementIndex("feature")).kind)
        val route = Routing.ManagedContent("peer", "public-key", AppFeatureType.NOTES.name)
        assertEquals(AppFeatureType.NOTES, AppFeatureType.valueOf(route.feature))
    }
}
