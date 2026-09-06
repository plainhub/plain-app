package com.ismartcoding.plain.db

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards against the "stale StateFlow list" bug class (2026-09-07):
 *
 * `data class` equals/hashCode only cover PRIMARY CONSTRUCTOR parameters. Any
 * Room column declared as a mutable body property (or inherited from a base
 * class) is invisible to equals, so `MutableStateFlow<List<Entity>>` treats a
 * reloaded list as equal to the old one and silently drops the emission — the
 * UI then shows stale data until process restart.
 *
 * Rule: every @Entity must declare ALL of its state as primary constructor
 * parameters (computed `val x get() = ...` without a backing field are fine).
 * When adding a new @Entity, add it to [entities].
 */
class EntityConstructorGuardTest {

    private val entities: List<KClass<*>> = listOf(
        DAppFile::class, DArchivedConversation::class, DBook::class, DBookChapter::class,
        DBookmark::class, DBookmarkGroup::class, DChat::class, DChatChannel::class,
        DFeed::class, DFeedEntry::class, DImageEditorProject::class, DImageEmbedding::class,
        DMediaItem::class, DNote::class, DPeer::class, DPomodoroItem::class,
        DSession::class, DShare::class, DTag::class, DTagRelation::class,
        DVideoPlayProgress::class,
    )

    @Test
    fun entitiesKeepAllStateInPrimaryConstructor() {
        val offenders = entities.mapNotNull { k ->
            val ctor = k.primaryConstructor ?: return@mapNotNull "${k.simpleName}: no primary constructor"
            val paramNames = ctor.parameters.mapNotNull { it.name }.toSet()
            k.declaredMemberProperties.forEach { it.isAccessible = true }
            // Any declared property outside the constructor that has a backing field is state.
            val withFields = k.declaredMemberProperties
                .filter { it.name !in paramNames }
                .filter { prop ->
                    try {
                        prop.javaField != null
                    } catch (_: Throwable) {
                        true // cannot inspect -> treat as state to be safe
                    }
                }
                .map { it.name }
            if (withFields.isNotEmpty()) {
                "${k.simpleName}: body state properties ${withFields} are not constructor parameters -> " +
                    "data class equals() ignores them, StateFlow will drop updates"
            } else null
        }
        if (offenders.isNotEmpty()) fail(offenders.joinToString("\n"))
    }

    @Test
    fun dataClassEqualsCoversAllColumnBearingFields() {
        // Secondary structural check: mutating any constructor property of a fresh
        // entity must change equals (catches accidental `get() =` constants too).
        val a = DNote(id = "guard")
        val b = a.copy()
        assertTrue(a == b, "copied DNotes must be equal")
        b.title = "different"
        assertTrue(a != b, "DNote.equals must include title (StateFlow relies on it)")
        b.title = a.title
        b.updatedAt = b.updatedAt.plus(kotlin.time.Duration.parse("1s"))
        assertTrue(a != b, "DNote.equals must include updatedAt")
    }
}
