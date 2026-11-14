package com.ismartcoding.plain.features.files

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FilePathResolverTest {
    @Test
    fun batchesAndDeduplicatesAliasesWithoutResolvingOrdinaryPaths() = runTest {
        val requests = mutableListOf<List<String>>()
        val resolver = FilePathResolver(backgroundScope, { paths -> requests.add(paths); paths.map { "/resolved/$it" } })
        val result = async { resolver.resolveAll(listOf("app://image", "fid:first", "app://image", "/ordinary", "https://example.invalid/image")) }
        runCurrent()
        assertEquals(listOf(listOf("app://image", "fid:first")), requests)
        assertEquals(listOf("/resolved/app://image", "/resolved/fid:first", "/resolved/app://image", "/ordinary", "https://example.invalid/image"), result.await())
    }

    @Test
    fun batchesAreBoundedAndCompletedResultsAreResolvedAgain() = runTest {
        val requests = mutableListOf<List<String>>()
        var currentPath: String? = "/original"
        val resolver = FilePathResolver(backgroundScope, { paths ->
            requests.add(paths)
            paths.map { currentPath }
        }, batchSize = 2)
        val result = async { resolver.resolveAll(listOf("fid:a", "fid:b", "fid:c")) }
        runCurrent()
        assertEquals(listOf(listOf("fid:a", "fid:b"), listOf("fid:c")), requests)
        assertEquals(listOf("/original", "/original", "/original"), result.await())

        currentPath = "/current"
        val changed = async { resolver.resolve("fid:a") }
        runCurrent()
        assertEquals("/current", changed.await())
        currentPath = null
        val removed = async { resolver.resolve("fid:a") }
        runCurrent()
        assertEquals("", removed.await())
        assertEquals(listOf(listOf("fid:a", "fid:b"), listOf("fid:c"), listOf("fid:a"), listOf("fid:a")), requests)
    }

    @Test
    fun cancelingOneWaiterDoesNotCancelTheSharedLookup() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val resolver = FilePathResolver(backgroundScope, { paths -> requests++; entered.complete(Unit); release.await(); paths.map { "/resolved" } })
        val first = async { resolver.resolve("fid:shared") }
        val second = async { resolver.resolve("fid:shared") }
        runCurrent()
        entered.await()
        first.cancelAndJoin()
        release.complete(Unit)
        runCurrent()
        assertEquals("/resolved", second.await())
        assertEquals(1, requests)
    }

    @Test
    fun missingResultsAndCanceledRequestsDoNotPoisonLaterBatches() = runTest {
        var requests = 0
        val resolver = FilePathResolver(backgroundScope, { paths ->
            requests++
            when (requests) {
                1 -> throw CancellationException("request canceled")
                2 -> paths.map { null }
                else -> paths.map { "/found" }
            }
        })
        val first = async { runCatching { resolver.resolve("fid:retry") } }
        runCurrent()
        assertIs<CancellationException>(first.await().exceptionOrNull())
        val second = async { resolver.resolve("fid:retry") }
        runCurrent()
        assertEquals("", second.await())
        val third = async { resolver.resolve("fid:retry") }
        runCurrent()
        assertEquals("/found", third.await())
        assertEquals(3, requests)
    }
}
