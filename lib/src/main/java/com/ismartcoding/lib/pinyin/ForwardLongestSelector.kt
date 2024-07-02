package com.ismartcoding.lib.pinyin

import com.ismartcoding.lib.ahocorasick.trie.Emit
import com.ismartcoding.lib.pinyin.Engine.EmitComparator
import java.util.*

/**
 * 正向最大匹配选择器
 */
internal class ForwardLongestSelector : SegmentationSelector {
    override fun select(emits: Collection<Emit>): List<Emit> {
        val results: MutableList<Emit> = ArrayList(emits)
        Collections.sort(results, HIT_COMPARATOR)
        var endValueToRemove = -1
        val emitToRemove: MutableSet<Emit> = TreeSet()
        for (emit in results) {
            if (emit.start > endValueToRemove && emit.end > endValueToRemove) {
                endValueToRemove = emit.end
            } else {
                emitToRemove.add(emit)
            }
        }
        results.removeAll(emitToRemove)
        return results
    }

    companion object {
        val HIT_COMPARATOR = EmitComparator()
    }
}
