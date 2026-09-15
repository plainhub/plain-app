package com.ismartcoding.plain.lib.codeeditor

enum class TokenKind { PLAIN, KEYWORD, TYPE, STRING, NUMBER, COMMENT, ANNOTATION, FUNCTION, OPERATOR, TAG, ATTRIBUTE, CONSTANT, VARIABLE }

data class Span(val start: Int, val end: Int, val kind: TokenKind)

/**
 * State-machine regex lexer in the spirit of ace mode rules: each state holds an ordered
 * rule list; the first regex matching at the cursor emits its token kind and optionally
 * transitions state. Lexing one line requires only the line text plus the state carried
 * over from the previous line, which is what makes incremental re-highlighting cheap.
 */
class RegexLexer(val languageId: String, states: List<StateDef>) {
    class Rule(val pattern: String, val kind: TokenKind, val push: String? = null, val pop: Boolean = false)
    class StateDef(val name: String, val rules: List<Rule>)

    private val stateNames = states.map { it.name }
    private val stateIndex = stateNames.withIndex().associate { (i, n) -> n to i }
    private val compiled = states.map { state ->
        state.rules.map { it.pattern.toRegex() } to state.rules
    }

    class LexResult(val spans: Array<Span>, val nextState: Int)

    fun lex(line: String, state: Int): LexResult {
        if (state == 0 && line.isEmpty()) return EMPTY_RESULT
        val spans = ArrayList<Span>(8)
        var pos = 0
        var current = state
        while (pos < line.length) {
            val (regexes, rules) = compiled[current]
            var matched = false
            for (i in rules.indices) {
                val result = regexes[i].matchAt(line, pos) ?: continue
                val rule = rules[i]
                val len = result.value.length
                if (len > 0) {
                    emitSpan(spans, pos, pos + len, rule.kind)
                    pos += len
                }
                current = when {
                    rule.pop -> 0
                    rule.push != null -> stateIndex[rule.push] ?: 0
                    else -> current
                }
                matched = true
                break
            }
            if (!matched) {
                emitSpan(spans, pos, pos + 1, TokenKind.PLAIN)
                pos++
                current = 0
            }
        }
        return LexResult(spans.toTypedArray(), current)
    }

    private fun emitSpan(spans: ArrayList<Span>, start: Int, end: Int, kind: TokenKind) {
        if (kind == TokenKind.PLAIN) return
        val last = spans.lastOrNull()
        if (last != null && last.end == start && last.kind == kind) {
            spans[spans.size - 1] = Span(last.start, end, kind)
        } else {
            spans.add(Span(start, end, kind))
        }
    }

    companion object {
        private val EMPTY_RESULT = LexResult(emptyArray(), 0)
    }
}

/** DSL for declaring languages as data. */
class LanguageBuilder(val id: String) {
    private val states = ArrayList<RegexLexer.StateDef>()

    fun state(name: String, block: StateBuilder.() -> Unit) {
        val builder = StateBuilder(name)
        builder.block()
        states.add(RegexLexer.StateDef(name, builder.rules))
    }

    fun build(): RegexLexer = RegexLexer(id, states)

    class StateBuilder internal constructor(val name: String) {
        internal val rules = ArrayList<RegexLexer.Rule>()

        fun rule(pattern: String, kind: TokenKind, push: String? = null, pop: Boolean = false) {
            rules.add(RegexLexer.Rule(pattern, kind, push, pop))
        }
    }
}

fun language(id: String, block: LanguageBuilder.() -> Unit): RegexLexer {
    val builder = LanguageBuilder(id)
    builder.block()
    return builder.build()
}
