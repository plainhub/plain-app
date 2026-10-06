package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Rust HTTP listener cannot read the APK top level, so [RustWebAssets]
 * unpacks the bundle from the assets dir. If the build stops shipping
 * `src/main/resources` under assets, every web route 404s while the host
 * unit tests stay green — `AssetManager` does not exist on the JVM.
 */
@RunWith(AndroidJUnit4::class)
class WebBundleAssetsTest {

    private val assets get() = InstrumentationRegistry.getInstrumentation().targetContext.assets

    @Test
    fun theWebBundleIsReadableThroughAssetManager() {
        val children = assets.list("web")
        assertNotNull("assets.list(\"web\") returned null — the bundle is not packaged as assets", children)
        assertTrue(
            "assets/web is empty — the bundle is only in the APK java-resources top level, " +
                "so RustWebAssets cannot unpack it and every web route will 404",
            children!!.isNotEmpty(),
        )
    }

    @Test
    fun theSpaEntryPointIsPresent() {
        assets.open("web/index.html").use { stream ->
            val body = stream.readBytes().decodeToString()
            assertTrue("web/index.html does not look like the SPA shell", body.contains("<html"))
        }
    }

    @Test
    fun hashedAssetBundlesArePresent() {
        val assets = assets.list("web/assets")!!
        assertTrue("web/assets is empty", assets.isNotEmpty())
        assertTrue(
            "no hashed JS bundle under web/assets — the SPA cannot boot",
            assets.any { it.endsWith(".js") },
        )
    }

    @Test
    fun unpackingProducesAReadableRoot() {
        val root = com.ismartcoding.plain.platform.RustWebAssets.ensure()
        assertTrue("RustWebAssets.ensure() returned an empty root", root.isNotEmpty())
        val index = java.io.File(root, "index.html")
        assertTrue("unpacked bundle has no index.html at $index", index.isFile)
    }
}
