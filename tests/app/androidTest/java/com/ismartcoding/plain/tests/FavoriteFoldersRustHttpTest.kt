package com.ismartcoding.plain.tests

import kotlinx.serialization.json.JsonObject
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.jsonObject
import com.ismartcoding.plain.features.files.FavoriteFoldersClient
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteFoldersRustHttpTest {
    @Test
    fun favoriteFoldersUseRustAndRefreshNativeCacheAfterExternalMutations() = runBlocking {
        val root = "/synthetic/favorites-${UUID.randomUUID()}"
        val first = "$root/photos 'quoted' \"test\" \\literal "
        val second = "$root/videos"
        try {
            FavoriteFoldersClient.add(root,first)
            FavoriteFoldersClient.add(root,second)
            FavoriteFoldersClient.rename(first,"  旅行  ")
            val updated = FavoriteFoldersClient.add(first,first).filter { it.fullPath.startsWith(root) }
            assertEquals(listOf(second,first),updated.map { it.fullPath })
            assertEquals("旅行",updated.last().alias)
            assertEquals(first,updated.last().rootPath)
            assertFalse(GraphqlApi.execute(GraphqlRequest<JsonObject>("query { prefs }")).getValue("prefs").jsonObject["favorite_folders"]?.toString().orEmpty().contains(root))
            var failed = false
            try { FavoriteFoldersClient.add(root,"$root-outside/folder") }
            catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertEquals(2,FavoriteFoldersClient.refresh().count { it.fullPath.startsWith(root) })
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + "setFavoriteFolderAlias(fullPath: ${JsonPrimitive(first)}, alias: ${JsonPrimitive("External change")}) { fullPath alias }" + " }"))
            withTimeout(5_000) {
                while (FavoriteFoldersClient.items.value.none { it.fullPath == first && it.alias == "External change" }) delay(50)
            }
            FavoriteFoldersClient.rename(first,"   ")
            assertNull(FavoriteFoldersClient.refresh().single { it.fullPath == first }.alias)
            FavoriteFoldersClient.remove(first)
            assertFalse(FavoriteFoldersClient.items.value.any { it.fullPath == first })
            assertEquals(second,FavoriteFoldersClient.refresh().single { it.fullPath.startsWith(root) }.fullPath)
        } finally {
            FavoriteFoldersClient.remove(first)
            FavoriteFoldersClient.remove(second)
        }
    }
}
