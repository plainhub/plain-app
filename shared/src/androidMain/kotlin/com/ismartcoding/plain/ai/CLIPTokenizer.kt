package com.ismartcoding.plain.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer

class CLIPTokenizer(tokenizerFile: File) {
    private val vocab = mutableMapOf<String, Int>()
    private val mergeRanks = mutableMapOf<Pair<String, String>, Int>()

    init {
        val json = JSONObject(tokenizerFile.readText())
        val model = json.getJSONObject("model")
        val vocabObj = model.getJSONObject("vocab")
        for (key in vocabObj.keys()) vocab[key] = vocabObj.getInt(key)
        val mergesArr = model.getJSONArray("merges")
        for (i in 0 until mergesArr.length()) {
            val element = mergesArr.get(i)
            val left: String
            val right: String
            if (element is JSONArray) {
                if (element.length() != 2) continue
                left = element.getString(0)
                right = element.getString(1)
            } else {
                val parts = element.toString().split(" ", limit = 2)
                if (parts.size != 2) continue
                left = parts[0]
                right = parts[1]
            }
            mergeRanks[Pair(left, right)] = i
        }
    }

    fun encode(text: String, maxLen: Int = 77): IntArray {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace(Regex("\\s+"), " ").trim().lowercase()
        val words = WORD_REGEX.findAll(normalized).map { it.value }.toList()
        val ids = mutableListOf(SOT_ID)
        for (word in words) {
            for (t in bpeEncode(word)) {
                val id = vocab[t] ?: continue
                ids.add(id)
                if (ids.size >= maxLen - 1) break
            }
            if (ids.size >= maxLen - 1) break
        }
        ids.add(EOT_ID)
        return IntArray(maxLen) { if (it < ids.size) ids[it] else 0 }
    }

    private fun bpeEncode(word: String): List<String> {
        val byteChars = word.toByteArray(Charsets.UTF_8)
            .map { BYTE_TO_UNICODE[it.toInt() and 0xFF]!! }
        if (byteChars.size == 1) return listOf(byteChars[0] + "</w>")
        val pieces = byteChars.toMutableList()
        pieces[pieces.lastIndex] = pieces.last() + "</w>"
        while (pieces.size > 1) {
            var bestIdx = -1
            var bestRank = Int.MAX_VALUE
            for (i in 0 until pieces.size - 1) {
                val rank = mergeRanks[Pair(pieces[i], pieces[i + 1])]
                if (rank != null && rank < bestRank) { bestRank = rank; bestIdx = i }
            }
            if (bestIdx < 0) break
            pieces[bestIdx] = pieces[bestIdx] + pieces[bestIdx + 1]
            pieces.removeAt(bestIdx + 1)
        }
        return pieces
    }

    companion object {
        private const val SOT_ID = 49406
        private const val EOT_ID = 49407
        private val WORD_REGEX = Regex(
            """<\|startoftext\|>|<\|endoftext\|>|'s|'t|'re|'ve|'m|'ll|'d|[\p{L}]+|[\p{N}]|[^\s\p{L}\p{N}]+""",
        )
        val BYTE_TO_UNICODE: Map<Int, String> by lazy {
            val bs = mutableListOf<Int>()
            bs.addAll('!'.code..'~'.code)
            bs.addAll('¡'.code..'¬'.code)
            bs.addAll('®'.code..'ÿ'.code)
            val cs = bs.map { it }.toMutableList()
            var n = 0
            for (b in 0..255) {
                if (b !in bs) { bs.add(b); cs.add(256 + n); n++ }
            }
            bs.zip(cs.map { Char(it).toString() }).toMap()
        }
    }
}
