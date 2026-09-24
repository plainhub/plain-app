package com.ismartcoding.plain

import com.ismartcoding.plain.httpserver.MainGraphQLService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Locks the wire contract of the main GraphQL schema:
 *
 * 1. The committed SDL snapshot (`shared/apitest/schema.graphqls`) must match the
 *    runtime schema byte for byte — schema changes are only landed by running
 *    [PrintSchemaTest] and committing the regenerated file.
 * 2. Structural conventions from `shared/apitest/API_SPEC.md` are enforced on the
 *    SDL so new fields/operations cannot drift from the spec.
 *
 * When this test fails after an intentional schema change: run
 * `./gradlew :shared:testAndroidHostTest --tests "com.ismartcoding.plain.PrintSchemaTest"`
 * to regenerate the snapshot, and update the convention allowlists only if the
 * spec document was updated with the same decision.
 */
class ApiContractTest {
    private val sdl: String = MainGraphQLService.create().schema.printSDL()

    // Fields that keep a raw String id although the naming rule says ID.
    // Each entry must be justified in API_SPEC.md ("deliberate exceptions").
    private val stringIdAllowlist = setOf(
        // Each entry must be justified in API_SPEC.md ("deliberate exceptions").
        "photoId", // Android RawContact photo row id (Contact/Call, Android-only feature)
        "thumbnailId",
        "clientId", // App.clientId — public client identity string, not an addressable entity id
        "rawId", // FeedEntry.rawId — upstream RSS guid, foreign identifier
        "requestId", // sendSms request id — idempotency key, not an entity id
        "pointerId", // multi-touch pointer slot index, not an entity id
        "ids", // ChatFiles/ChatImages.ids — app file store fileIds (2026-09-20 user: String, not ID)
        "linkPreviewImageIds", // ChatText — cached link-preview image fileIds (String)
        "subscriptionId", // Android SIM subscription integer (slot index), not an entity id
        "diskId", // StorageMount.diskId — OS disk uuid, foreign identifier (empty on Android)
        // fileId value space is String, never ID (2026-09-21 user decision: all fileIds are String).
        "albumFileId", // Audio.albumFileId — album-art display fileId token
        "AppFile", // AppFile.id — content-addressable app file store fileId (same space as ChatFiles.ids)
        "fileId", // chunk-flow args (uploadedChunks/mergeStatus/deleteChunks/mergeChunks/mergeAppFileChunks)
    )

    // Bulk destructive/modify mutations that must return ActionResult.
    private val actionResultMutations = setOf(
        "deleteNotifications", "deleteClipboards", "deleteBookmarks", "deleteFiles",
        "deleteSms", "trashSms", "restoreSms", "deleteCalls", "deleteContacts",
        "deleteChatItems", "trashNotes", "restoreNotes", "deleteNotes",
        "deleteFeedEntries", "deleteMediaItems", "trashMediaItems",
        "restoreMediaItems", "moveMediaItems",
    )

    // Query-addressed bulk mutations: the server rejects a blank query
    // (QueryHelper.requireExplicitBulkQuery); whole-table intent is the explicit
    // `all:true` sentinel (API_SPEC §5). deleteChatItems is guarded by its own
    // empty-parse → empty-ids precedent (ChatDbHelper).
    private val bulkQueryMutations = setOf(
        "deleteMediaItems", "trashMediaItems", "restoreMediaItems", "moveMediaItems",
        "trashNotes", "restoreNotes", "deleteNotes", "saveFeedEntriesToNotes",
        "deleteFeedEntries", "trashSms", "restoreSms", "deleteSms",
        "deleteCalls", "deleteContacts", "deleteChatItems",
    )

    @Test
    fun sdlSnapshotMatchesCommittedFile() {
        val committed = java.io.File("apitest/schema.graphqls").readText()
        if (committed != sdl) {
            fail(
                "shared/apitest/schema.graphqls is out of date with the runtime schema. " +
                    "Regenerate with PrintSchemaTest and commit the result (see API_SPEC.md).",
            )
        }
    }

    @Test
    fun idSuffixedFieldsUseTheIdScalar() {
        forEachTypedField { (owner, name, type) ->
            val isIdName = isIdName(name)
            if (!isIdName) return@forEachTypedField
            val usesIdScalar = type.startsWith("ID") || type.startsWith("[ID")
            if (!usesIdScalar && owner !in stringIdAllowlist && name !in stringIdAllowlist) {
                fail("Field $owner.$name is typed '$type' — id-suffixed fields must use the ID scalar (spec §IDs).")
            }
        }
    }

    @Test
    fun idSuffixedArgumentsUseTheIdScalar() {
        forEachOperationArgument { (op, name, type) ->
            val isIdName = isIdName(name)
            if (!isIdName) return@forEachOperationArgument
            val usesIdScalar = type.startsWith("ID") || type.startsWith("[ID")
            if (!usesIdScalar && name !in stringIdAllowlist && name != "ids") { // deleteDbTableRows(ids): raw table PKs, debug-only
                fail("Argument $op($name:) is typed '$type' — id-suffixed arguments must use the ID scalar (spec §IDs).")
            }
        }
    }

    @Test
    fun bulkMutationsReturnActionResult() {
        val returns = mutationReturnTypes()
        actionResultMutations.forEach { op ->
            val type = returns[op] ?: fail("Mutation $op disappeared from the schema — update ApiContractTest.actionResultMutations.")
            assertEquals("ActionResult!", type, "Bulk mutation $op must return ActionResult! (spec §bulk operations).")
        }
        // And nothing else may return the legacy shapes.
        returns.forEach { (op, type) ->
            if (op in actionResultMutations && type != "ActionResult!") {
                fail("Bulk mutation $op returns $type — must be ActionResult!.")
            }
        }
    }

    @Test
    fun bulkQueryMutationsKeepQueryRequired() {
        val signatures = operationSignaturesIn("Mutation")
        bulkQueryMutations.forEach { op ->
            val sig = signatures.firstOrNull { it.startsWith("$op(") }
                ?: fail("Mutation $op disappeared from the schema — update ApiContractTest.bulkQueryMutations.")
            if (!sig.contains("query: String!")) {
                fail("Mutation $sig must declare query: String! — the empty-query bulk guard relies on a required query (spec §5).")
            }
        }
        // Registry drift: every ActionResult mutation addressed by query (plus
        // saveFeedEntriesToNotes) must be registered here so it gets the blank-query guard.
        val queryShaped = signatures
            .filter { it.substringBefore("(").trim() in actionResultMutations }
            .filter { it.contains("query: String!") }
            .map { it.substringBefore("(").trim() }
            .toSet()
        val expected = queryShaped + setOf("saveFeedEntriesToNotes")
        assertEquals(expected, bulkQueryMutations,
            "Query-shaped bulk mutations drifted — update bulkQueryMutations and guard the new entry (spec §5).")
    }

    @Test
    fun paginatedListsAlwaysPairOffsetLimitAndQuery() {
        forEachQuerySignature { signature ->
            if (signature.contains("offset")) {
                if (!signature.contains("offset: Int!")) fail("Query $signature — offset must be Int!.")
                if (!signature.contains("limit: Int!")) fail("Query $signature — paginated query must declare limit: Int!.")
                // dbTableRows is the debug DB browser — deliberately exempt (2026-09-20 user decision: debug APIs stay as-is).
                if (!signature.contains("query: String!") && !signature.startsWith("dbTableRows(")) fail("Query $signature — paginated query must declare query: String! (spec §3).")
            }
            if (signature.contains("limit") && !signature.contains("offset")) {
                fail("Query $signature — limit without offset.")
            }
        }
    }

    @Test
    fun durationsAndSizesCarryUnitsAnd64BitWidth() {
        // Bare duration fields are forbidden — use durationMs / durationSec / *Min / *Sec.
        forbiddenPattern("""\bduration:""".trim()) { "bare duration is forbidden — use durationMs/durationSec — $it" }
        forbiddenPattern("""\btimeLeft(?!Sec):""".trim()) { "use timeLeftSec — $it" }
        forbiddenPattern("""\btotalTime(?!Sec):""".trim()) { "use totalTimeSec — $it" }
        forbiddenPattern("""\bworkDuration(?!Min):""".trim()) { "use workDurationMin — $it" }
        forbiddenPattern("""\bshortBreakDuration(?!Min):""".trim()) { "use shortBreakDurationMin — $it" }
        forbiddenPattern("""\blongBreakDuration(?!Min):""".trim()) { "use longBreakDurationMin — $it" }
        // Sizes/durations over 2 GiB overflow GraphQL Int (32-bit).
        forbiddenPattern("""\bsize: Int\b""".trim()) { "size must be Long — $it" }
    }

    @Test
    fun timestampsAreInstantsNotRawNumbersOrStrings() {
        forbiddenPattern("""\w+At: (Long|String)""".trim()) { "timestamp fields must use the Instant scalar — $it" }
    }

    @Test
    fun operationsReturningEntityIdsUseIdScalar() {
        // Operations whose return value is a list of entity ids must use [ID!]! —
        // raw [String!]! return shapes bypass the id-suffix field check.
        val returns = mutationReturnTypes()
        assertEquals("[ID!]!", returns["saveFeedEntriesToNotes"],
            "saveFeedEntriesToNotes returns feed-entry ids — must be [ID!]! (spec §1/§4).")
    }

    @Test
    fun countSiblingsCarryTheListFilter() {
        val querySigs = operationSignaturesIn("Query")
        querySigs.forEach { sig ->
            if (!sig.contains("offset")) return@forEach
            val list = sig.substringBefore("(")
            val countSig = querySigs.firstOrNull { it.startsWith("${list}Count(") } ?: return@forEach
            if (!countSig.contains("query: String!")) fail("Query $countSig must declare query: String! — sibling of paginated $sig (spec §3).")
        }
    }

    @Test
    fun legacyShapesAreGone() {
        forbiddenPattern("MediaActionResult") { "legacy MediaActionResult must not come back — use ActionResult — $it" }
        forbiddenPattern("smsAllCounts") { "renamed to smsBoxCounts — $it" }
        forbiddenPattern("feedsCount") { "renamed to feedEntryCounts — $it" }
        forbiddenPattern("topItems") { "renamed to topItemPaths — $it" }
        forbiddenPattern("fetchFeedContent") { "duplicate of syncFeedEntryContent was removed — $it" }
        // 2026-09-24 naming cleanup (API_SPEC §7) — old names must not come back.
        forbiddenPattern("filesCount") { "renamed to fileCount — $it" }
        forbiddenPattern("screenMirrorState") { "renamed to isScreenMirroring — $it" }
        forbiddenPattern("syncFeedContent") { "renamed to syncFeedEntryContent (the id is a feed-entry id) — $it" }
        forbiddenPattern("renameAudioPlaylist") { "renamed to updateAudioPlaylist (returns the entity) — $it" }
        forbiddenPattern("archiveConversation") { "renamed to archiveSmsConversation/unarchiveSmsConversation — $it" }
        forbiddenPattern("""\bchildren:""") { "File.children was renamed to childCount (it is a count, not a collection) — $it" }
        forbiddenPattern("""\bdeleteClipboard\b""".trim()) { "renamed to deleteClipboards (bulk delete is plural) — $it" }
        forbiddenPattern("""\barchivedConversations\b""".trim()) { "renamed to archivedSmsConversations (Sms prefix, aligned with smsConversations) — $it" }
    }

    // ------------------------------------------------------------------
    // SDL parsing helpers (line-oriented; the printer emits one field per line).
    // ------------------------------------------------------------------

    private fun forEachTypedField(block: (Triple<String, String, String>) -> Unit) {
        var owner = ""
        sdl.lineSequence().forEach { raw ->
            val line = raw.trim()
            val typeHeader = Regex("^type (\\w+)( implements.*)? \\{$").find(line)
            val inputHeader = Regex("^input (\\w+) \\{$").find(line)
            if (typeHeader != null) owner = typeHeader.groupValues[1]
            if (inputHeader != null) owner = inputHeader.groupValues[1]
            if (line == "}") owner = ""
            val field = Regex("^(\\w+): ([^ ]+.*),?$").find(line)
            if (field != null && owner.isNotEmpty() && !line.startsWith("\"")) {
                block(Triple(owner, field.groupValues[1], field.groupValues[2].trimEnd(',')))
            }
        }
    }

    private fun forEachOperationArgument(block: (Triple<String, String, String>) -> Unit) {
        operationSignatures().forEach { signature ->
            val opName = signature.substringBefore("(").trim()
            signature.substringAfter("(", "").substringBeforeLast(")").split(", ").forEach { arg ->
                val name = arg.substringBefore(":").trim()
                val type = arg.substringAfter(":", "").trim()
                if (name.isNotEmpty() && type.isNotEmpty()) block(Triple(opName, name, type))
            }
        }
    }

    private fun forEachQuerySignature(block: (String) -> Unit) {
        sdl.lineSequence().map { it.trim() }.filter { it.contains("(") && it.contains("):") }
            .forEach { block(it.trimEnd(',')) }
    }

    private fun mutationReturnTypes(): Map<String, String> =
        operationSignatures().mapNotNull { signature ->
            if (!signature.contains("(")) return@mapNotNull null
            val name = signature.substringBefore("(").trim()
            val ret = signature.substringAfterLast("): ").trimEnd(',')
            name to ret
        }.toMap()

    /** Lines of the Query/Mutation blocks that declare an operation (contain a `(`). */
    private fun operationSignatures(): List<String> {
        val lines = sdl.lineSequence().map { it.trim() }.toList()
        val out = mutableListOf<String>()
        var inOperationBlock = false
        lines.forEach { line ->
            if (line == "type Query {" || line == "type Mutation {") inOperationBlock = true
            if (line == "}") inOperationBlock = false
            if (inOperationBlock && line.contains("(")) out.add(line.trimEnd(','))
        }
        return out
    }

    /** id/id-ish names, including casing drift like `diskID` (spec §IDs). */
    private fun isIdName(name: String): Boolean =
        name == "id" || name.endsWith("Id") || name.endsWith("Ids") || name.endsWith("IDs") || name.endsWith("ID")

    /** Lines of one top-level operation block (Query/Mutation) that declare an operation. */
    private fun operationSignaturesIn(block: String): List<String> {
        var inside = false
        val out = mutableListOf<String>()
        sdl.lineSequence().map { it.trim() }.forEach { line ->
            if (line == "type $block {") inside = true
            if (inside && line == "}") inside = false
            if (inside && line.contains("(")) out.add(line.trimEnd(','))
        }
        return out
    }

    private fun forbiddenPattern(pattern: String, message: (String) -> String) {
        val regex = Regex(pattern)
        sdl.lineSequence().map { it.trim() }.filter { regex.containsMatchIn(it) }.firstOrNull()?.let {
            fail(message(it))
        }
    }
}
