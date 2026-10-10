package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import com.ismartcoding.plain.features.FavoriteFolderHelper
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FavoriteFoldersRustHttpTest {
    @Test
    fun favoriteFoldersUseRustAndRefreshNativeCacheAfterExternalMutations() = runBlocking {
        val root = "/synthetic/favorites-${UUID.randomUUID()}"
        val first = "$root/photos 'quoted' \"test\" \\literal "
        val second = "$root/videos"
        try {
            FavoriteFolderHelper.add(root,first)
            FavoriteFolderHelper.add(root,second)
            FavoriteFolderHelper.rename(first,"  旅行  ")
            val updated = FavoriteFolderHelper.add(first,first).filter { it.fullPath.startsWith(root) }
            assertEquals(listOf(second,first),updated.map { it.fullPath })
            assertEquals("旅行",updated.last().alias)
            assertEquals(first,updated.last().rootPath)
            assertFalse(RustContentApi.query("userPrefs").getValue("userPrefs").jsonObject["favorite_folders"]?.toString().orEmpty().contains(root))
            var failed = false
            try { FavoriteFolderHelper.add(root,"$root-outside/folder") }
            catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertEquals(2,FavoriteFolderHelper.refresh().count { it.fullPath.startsWith(root) })
            RustContentApi.mutate("setFavoriteFolderAlias(fullPath: ${JsonPrimitive(first)}, alias: ${JsonPrimitive("External change")}) { fullPath alias }")
            withTimeout(5_000) {
                while (FavoriteFolderHelper.items.value.none { it.fullPath == first && it.alias == "External change" }) delay(50)
            }
            FavoriteFolderHelper.rename(first,"   ")
            assertNull(FavoriteFolderHelper.refresh().single { it.fullPath == first }.alias)
            FavoriteFolderHelper.remove(first)
            assertFalse(FavoriteFolderHelper.items.value.any { it.fullPath == first })
            assertEquals(second,FavoriteFolderHelper.refresh().single { it.fullPath.startsWith(root) }.fullPath)
        } finally {
            FavoriteFolderHelper.remove(first)
            FavoriteFolderHelper.remove(second)
        }
    }
}
