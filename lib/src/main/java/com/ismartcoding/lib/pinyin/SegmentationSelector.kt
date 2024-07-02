package com.ismartcoding.lib.pinyin

import com.ismartcoding.lib.ahocorasick.trie.Emit

/**
 * 分词选择算法应实现的接口
 *
 * TODO: 开放此接口，提供更多的预制Selector，也可供使用者实现自定义Selector
 *
 */
interface SegmentationSelector {
    /**
     * 从匹配到的所有词中，挑选出合适的词列表
     *
     * 例如，词典为："中国"，"中国人"，"人民"
     * "中国人民来了" 的emits为：[0,1]中国、[0,2]中国人、[2,3]人民
     *
     * 按照正向最大匹配算法，会挑选出：[0,2]中国人
     *
     * Waring:  返回的Emit列表中，不能存在交集，如上例中，不能同时返回 [0,2]中国人 和 [2,3]人民
     *
     * @param emits 匹配到的所有词
     * @return 返回的Emit列表, 不能存在交集
     */
    fun select(emits: Collection<Emit>): List<Emit>
}
