package com.ismartcoding.lib.pdfviewer

import android.graphics.RectF
import com.ismartcoding.lib.pdfviewer.model.PagePart
import com.ismartcoding.lib.pdfviewer.util.Constants.CACHE_SIZE
import com.ismartcoding.lib.pdfviewer.util.Constants.THUMBNAILS_CACHE_SIZE
import java.util.*

class CacheManager {
    private val orderComparator = PagePartComparator()
    private val passiveCache = PriorityQueue(CACHE_SIZE, orderComparator)
    private val activeCache = PriorityQueue(CACHE_SIZE, orderComparator)
    private val thumbnails = mutableListOf<PagePart>()
    private val passiveActiveLock = Any()

    fun cachePart(part: PagePart) {
        synchronized(passiveActiveLock) {
            // If cache too big, remove and recycle
            makeAFreeSpace()

            // Then add part
            activeCache.offer(part)
        }
    }

    fun makeANewSet() {
        synchronized(passiveActiveLock) {
            passiveCache.addAll(activeCache)
            activeCache.clear()
        }
    }

    private fun makeAFreeSpace() {
        synchronized(passiveActiveLock) {
            while ((activeCache.size + passiveCache.size) >= CACHE_SIZE &&
                !passiveCache.isEmpty()
            ) {
                passiveCache.poll()?.renderedBitmap?.recycle()
            }
            while ((activeCache.size + passiveCache.size) >= CACHE_SIZE &&
                !activeCache.isEmpty()
            ) {
                activeCache.poll()?.renderedBitmap?.recycle()
            }
        }
    }

    fun cacheThumbnail(part: PagePart) {
        synchronized(thumbnails) {
            // If cache too big, remove and recycle
            while (thumbnails.size >= THUMBNAILS_CACHE_SIZE) {
                thumbnails.removeAt(0).renderedBitmap?.recycle()
            }

            // Then add thumbnail
            addWithoutDuplicates(thumbnails, part)
        }
    }

    fun upPartIfContained(
        page: Int,
        pageRelativeBounds: RectF,
        toOrder: Int,
    ): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, false, 0)
        var found: PagePart?
        synchronized(passiveActiveLock) {
            if (find(passiveCache, fakePart).also { found = it } != null) {
                passiveCache.remove(found)
                found?.cacheOrder = toOrder
                activeCache.offer(found)
                return true
            }
            return find(activeCache, fakePart) != null
        }
    }

    /**
     * Return true if already contains the described PagePart
     */
    fun containsThumbnail(
        page: Int,
        pageRelativeBounds: RectF,
    ): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, true, 0)
        synchronized(thumbnails) {
            for (part in thumbnails) {
                if (part == fakePart) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Add part if it doesn't exist, recycle bitmap otherwise
     */
    private fun addWithoutDuplicates(
        collection: MutableCollection<PagePart>,
        newPart: PagePart,
    ) {
        for (part in collection) {
            if (part == newPart) {
                newPart.renderedBitmap?.recycle()
                return
            }
        }
        collection.add(newPart)
    }

    val pageParts: List<PagePart>
        get() {
            synchronized(passiveActiveLock) {
                val parts = passiveCache.toMutableList()
                parts.addAll(activeCache)
                return parts
            }
        }

    fun getThumbnails(): List<PagePart> {
        synchronized(thumbnails) { return thumbnails }
    }

    fun recycle() {
        synchronized(passiveActiveLock) {
            for (part in passiveCache) {
                part.renderedBitmap?.recycle()
            }
            passiveCache.clear()
            for (part in activeCache) {
                part.renderedBitmap?.recycle()
            }
            activeCache.clear()
        }
        synchronized(thumbnails) {
            for (part in thumbnails) {
                part.renderedBitmap?.recycle()
            }
            thumbnails.clear()
        }
    }

    internal inner class PagePartComparator : Comparator<PagePart> {
        override fun compare(
            part1: PagePart,
            part2: PagePart,
        ): Int {
            if (part1.cacheOrder == part2.cacheOrder) {
                return 0
            }
            return if (part1.cacheOrder > part2.cacheOrder) 1 else -1
        }
    }

    companion object {
        private fun find(
            vector: PriorityQueue<PagePart>,
            fakePart: PagePart,
        ): PagePart? {
            for (part in vector) {
                if (part.equals(fakePart)) {
                    return part
                }
            }
            return null
        }
    }
}
