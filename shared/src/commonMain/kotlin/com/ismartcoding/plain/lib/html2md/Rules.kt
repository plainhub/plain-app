package com.ismartcoding.plain.lib.html2md

internal class Rules {
    val options = Options()
    val rules = mutableListOf<Rule>()
    val references = mutableListOf<String>()

    init {
        addRule(
            "blankReplacement",
            Rule(
                { element: HtmlNode -> ProcessNode.isBlank(element) },
            ) { _: String, node: HtmlNode -> if (ProcessNode.isBlock(node)) "\n\n" else "" },
        )
        addRule(
            "paragraph",
            Rule("p") { content: String, _: HtmlNode ->
                "\n\n$content\n\n"
            },
        )
        addRule(
            "br",
            Rule("br") { _: String, _: HtmlNode ->
                "${options.br}\n"
            },
        )
        addRule(
            "heading",
            Rule(setOf("h1", "h2", "h3", "h4", "h5", "h6")) { content: String, node: HtmlNode ->
                val hLevel = node.nodeName().substring(1, 2).toInt()
                if (options.headingStyle == HeadingStyle.SETEXT && hLevel < 3) {
                    val underline = (if (hLevel == 1) "=" else "-").repeat(content.length)
                    "\n\n$content\n$underline\n\n"
                } else {
                    "\n\n${"#".repeat(hLevel)} $content\n\n"
                }
            },
        )
        addRule(
            "blockquote",
            Rule("blockquote") { content: String, _: HtmlNode ->
                var c = content.replace("^\n+|\n+$".toRegex(), "")
                c = c.replace("(?m)^".toRegex(), "> ")
                "\n\n$c\n\n"
            },
        )
        addRule(
            "list",
            Rule(setOf("ul", "ol")) { content: String, node: HtmlNode ->
                val parent = node.parentNode() as HtmlElement?
                if (parent?.nodeName() == "li" && parent.child(parent.childrenSize() - 1) === node) {
                    "\n$content"
                } else {
                    "\n\n$content\n\n"
                }
            },
        )
        addRule(
            "listItem",
            Rule("li") { content: String, node: HtmlNode ->
                val c =
                    content.replace("^\n+".toRegex(), "")
                        .replace("\n+$".toRegex(), "\n")
                        .replace("(?m)\n".toRegex(), "\n    ")
                var prefix = options.bulletListMaker + "   "
                val parent = node.parentNode() as HtmlElement?
                if (parent!!.nodeName() == "ol") {
                    val start = parent.attr("start")
                    val index = parent.children().indexOf(node)
                    var parsedStart = 1
                    if (start.isNotEmpty()) {
                        try {
                            parsedStart = start.toInt()
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    prefix = (parsedStart + index).toString() + ".  "
                }
                prefix + c + if (node.nextSibling() != null && !Regex("\n$").containsMatchIn(c)) "\n" else ""
            },
        )
        addRule(
            "indentedCodeBlock",
            Rule({ node: HtmlNode ->
                options.codeBlockStyle == CodeBlockStyle.INDENTED && node.nodeName() == "pre" && node.firstChild()?.nodeName() == "code"
            }) { _: String, node: HtmlNode ->
                "\n\n    ${(node.firstChild() as HtmlElement).wholeText().replace("\n", "\n    ")}\n\n"
            },
        )
        addRule(
            "fencedCodeBock",
            Rule({ node: HtmlNode ->
                options.codeBlockStyle == CodeBlockStyle.FENCED && node.nodeName() == "pre" && node.firstChild()?.nodeName() == "code"
            }) { content: String, node: HtmlNode ->
                val first = node.childNode(0)
                val childClass = if (first is HtmlElement) first.attr("class") else ""
                val languageMatch = Regex("language-(\\S+)").find(childClass)
                var language = ""
                if (languageMatch != null) {
                    language = languageMatch.groupValues[1]
                }
                var code: String =
                    if (first is HtmlElement) {
                        first.wholeText()
                    } else {
                        first.outerHtml()
                    }
                val fenceChar = options.fence.substring(0, 1)
                var fenceSize = 3
                for (match in Regex("(?m)^($fenceChar{3,})").findAll(content)) {
                    val group = match.groupValues[1]
                    fenceSize = (group.length + 1).coerceAtLeast(fenceSize)
                }
                val fence = fenceChar.repeat(fenceSize)
                if (code.isNotEmpty() && code[code.length - 1] == '\n') {
                    code = code.substring(0, code.length - 1)
                }
                "\n\n$fence$language\n$code\n$fence\n\n"
            },
        )
        addRule(
            "horizontalRule",
            Rule("hr") { _: String, _: HtmlNode? ->
                "\n\n${options.hr}\n\n"
            },
        )
        addRule(
            "inlineLink",
            Rule(
                { node: HtmlNode -> options.linkStyle == LinkStyle.INLINED && node.nodeName() == "a" && node.attr("href").isNotEmpty() },
            ) { content: String, node: HtmlNode ->
                val href = node.attr("href")
                var title = cleanAttribute(node.attr("title"))
                if (title.isNotEmpty()) {
                    title = " \"$title\""
                }
                "[$content]($href$title)"
            },
        )
        addRule(
            "referenceLink",
            Rule(
                { node: HtmlNode -> options.linkStyle == LinkStyle.REFERENCED && node.nodeName() == "a" && node.attr("href").isNotEmpty() },
                { content: String, node: HtmlNode ->
                    val href = node.attr("href")
                    var title = cleanAttribute(node.attr("title"))
                    if (title.isNotEmpty()) {
                        title = " \"$title\""
                    }
                    val replacement: String
                    val reference: String
                    when (options.linkReferenceStyle) {
                        LinkReferenceStyle.COLLAPSED -> {
                            replacement = "[$content][]"
                            reference = "[$content]: $href$title"
                        }
                        LinkReferenceStyle.SHORTCUT -> {
                            replacement = "[$content]"
                            reference = "[$content]: $href$title"
                        }
                        LinkReferenceStyle.DEFAULT -> {
                            val id = references.size + 1
                            replacement = "[$content][$id]"
                            reference = "[$id]: $href$title"
                        }
                    }
                    references.add(reference)
                    replacement
                },
                {
                    var referenceString = ""
                    if (references.size > 0) {
                        referenceString = "\n\n${references.joinToString("\n")}\n\n"
                    }
                    referenceString
                },
            ),
        )
        addRule(
            "emphasis",
            Rule(setOf("em", "i")) { content: String, _: HtmlNode ->
                if (content.trim { it <= ' ' }.isEmpty()) {
                    ""
                } else {
                    options.emDelimiter + content + options.emDelimiter
                }
            },
        )
        addRule(
            "strong",
            Rule(setOf("strong", "b")) { content: String, _: HtmlNode ->
                if (content.isBlank()) {
                    ""
                } else {
                    options.strongDelimiter + content + options.strongDelimiter
                }
            },
        )
        addRule(
            "code",
            Rule({ node: HtmlNode ->
                val hasSiblings = node.previousSibling() != null || node.nextSibling() != null
                val isCodeBlock = node.parentNode()!!.nodeName() == "pre" && !hasSiblings
                node.nodeName() == "code" && !isCodeBlock
            }, { content: String, _: HtmlNode ->
                if (content.trim { it <= ' ' }.isEmpty()) {
                    ""
                } else {
                    var delimiter = "`"
                    var leadingSpace = ""
                    var trailingSpace = ""
                    val pattern = Regex("(?m)(`)+")
                    val matcher = pattern.find(content)
                    if (matcher != null) {
                        if (Regex("^`").containsMatchIn(content)) {
                            leadingSpace = " "
                        }
                        if (Regex("`$").containsMatchIn(content)) {
                            trailingSpace = " "
                        }
                        var counter = 1
                        if (delimiter == matcher.value) {
                            counter++
                        }
                        var allMatches = pattern.findAll(content).iterator()
                        allMatches.next() // skip first
                        while (allMatches.hasNext()) {
                            val m = allMatches.next()
                            if (delimiter == m.value) {
                                counter++
                            }
                        }
                        delimiter = "`".repeat(counter)
                    }
                    delimiter + leadingSpace + content + trailingSpace + delimiter
                }
            }),
        )
        addRule(
            "img",
            Rule("img") { _: String, node: HtmlNode ->
                val alt = cleanAttribute(node.attr("alt"))
                val src = node.attr("src")
                if (src.isEmpty()) {
                    ""
                } else {
                    val title = cleanAttribute(node.attr("title"))
                    var titlePart = ""
                    if (title.isNotEmpty()) {
                        titlePart = " \"$title\""
                    }
                    "![$alt]($src$titlePart)"
                }
            },
        )

        addRule(
            "strikethrough",
            Rule(setOf("del", "s", "strike")) { content: String, _: HtmlNode ->
                "~$content~"
            },
        )
        addRule(
            "taskListItems",
            Rule({ node: HtmlNode -> node.nodeName() == "input" && node.attr("type") == "checkbox" }) { _: String, node: HtmlNode ->
                if (node.hasAttr("checked")) {
                    "[x] "
                } else {
                    "[ ] "
                }
            },
        )

        addRule(
            "highlightedCodeBlock",
            Rule({ node: HtmlNode ->
                node.nodeName() == "div" &&
                    highlightRegExp.matches(node.attr("class")) &&
                    node.firstChild()?.nodeName() == "pre"
            }) { _: String, node: HtmlNode ->
                val className = node.attr("class")
                val language = highlightRegExp.matchEntire(className)?.groupValues?.get(1) ?: ""
                "\n\n${options.fence}$language\n${node.firstChild()?.outerHtml()}\n${options.fence}\n\n"
            },
        )

        addRule("tableCell", Table.tableCell())
        addRule("tableRow", Table.tableRow())
        addRule("table", Table.table())
        addRule("tableSection", Table.tableSection())
        addRule("tableCaption", Table.tableCaption())

        addRule(
            "default",
            Rule({ true }) { content: String, element: HtmlNode ->
                if (ProcessNode.isBlock(element)) "\n\n$content\n\n" else content
            },
        )
    }

    fun findRule(node: HtmlNode): Rule? {
        for (rule in rules) {
            if (rule.filter(node)) {
                return rule
            }
        }
        return null
    }

    private fun addRule(
        name: String,
        rule: Rule,
    ) {
        rule.name = name
        rules.add(rule)
    }

    private fun cleanAttribute(attribute: String): String {
        return attribute.replace("(\n+\\s*)+".toRegex(), "\n")
    }

    companion object {
        val highlightRegExp = Regex("highlight-(?:text|source)-([a-z0-9]+)")
    }
}
