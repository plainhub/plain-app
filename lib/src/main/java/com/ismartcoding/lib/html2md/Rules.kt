package com.ismartcoding.lib.html2md

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.util.*
import java.util.regex.Pattern

internal class Rules {
    val options = Options()
    val rules = mutableListOf<Rule>()
    val references = mutableListOf<String>()

    init {
        addRule(
            "blankReplacement",
            Rule(
                { element: Node -> ProcessNode.isBlank(element) },
            ) { _: String, node: Node -> if (ProcessNode.isBlock(node)) "\n\n" else "" },
        )
        addRule(
            "paragraph",
            Rule("p") { content: String, _: Node ->
                "\n\n$content\n\n"
            },
        )
        addRule(
            "br",
            Rule("br") { _: String, _: Node ->
                "${options.br}\n"
            },
        )
        addRule(
            "heading",
            Rule(setOf("h1", "h2", "h3", "h4", "h5", "h6")) { content: String, node: Node ->
                val hLevel = node.nodeName().substring(1, 2).toInt()
                if (options.headingStyle == HeadingStyle.SETEXT && hLevel < 3) {
                    val underline = java.lang.String.join("", Collections.nCopies(content.length, if (hLevel == 1) "=" else "-"))
                    "\n\n$content\n$underline\n\n"
                } else {
                    "\n\n${"#".repeat(hLevel)} $content\n\n"
                }
            },
        )
        addRule(
            "blockquote",
            Rule("blockquote") { content: String, _: Node ->
                var c = content.replace("^\n+|\n+$".toRegex(), "")
                c = c.replace("(?m)^".toRegex(), "> ")
                "\n\n$c\n\n"
            },
        )
        addRule(
            "list",
            Rule(setOf("ul", "ol")) { content: String, node: Node ->
                val parent = node.parentNode() as Element?
                if (parent?.nodeName() == "li" && parent.child(parent.childrenSize() - 1) == node) {
                    "\n$content"
                } else {
                    "\n\n$content\n\n"
                }
            },
        )
        addRule(
            "listItem",
            Rule("li") { content: String, node: Node ->
                val c =
                    content.replace("^\n+".toRegex(), "") // remove leading new lines
                        .replace("\n+$".toRegex(), "\n") // remove trailing new lines with just a single one
                        .replace("(?m)\n".toRegex(), "\n    ") // indent
                var prefix = options.bulletListMaker + "   "
                val parent = node.parentNode() as Element?
                if (parent!!.nodeName() == "ol") {
                    val start = parent.attr("start")
                    val index = parent.children().indexOf(node)
                    var parsedStart = 1
                    if (start.isNotEmpty()) {
                        try {
                            parsedStart = Integer.valueOf(start)
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    prefix = (parsedStart + index).toString() + ".  "
                }
                prefix + c + if (node.nextSibling() != null && !Pattern.compile("\n$").matcher(c).find()) "\n" else ""
            },
        )
        addRule(
            "indentedCodeBlock",
            Rule({ node: Node ->
                options.codeBlockStyle == CodeBlockStyle.INDENTED && node.nodeName() == "pre" && node.firstChild()?.nodeName() == "code"
            }) { _: String, node: Node ->
                "\n\n    ${(node.firstChild() as Element).wholeText().replace("\n", "\n    ")}\n\n"
            },
        )
        addRule(
            "fencedCodeBock",
            Rule({ node: Node ->
                options.codeBlockStyle == CodeBlockStyle.FENCED && node.nodeName() == "pre" && node.firstChild()?.nodeName() == "code"
            }) { content: String, node: Node ->
                val first = node.childNode(0)
                val childClass = first.attr("class")
                val languageMatcher = Pattern.compile("language-(\\S+)").matcher(childClass)
                var language = ""
                if (languageMatcher.find()) {
                    language = languageMatcher.group(1) as String
                }
                var code: String
                code =
                    if (first is Element) {
                        first.wholeText()
                    } else {
                        first.outerHtml()
                    }
                val fenceChar = options.fence.substring(0, 1)
                var fenceSize = 3
                val fenceMatcher = Pattern.compile("(?m)^($fenceChar{3,})").matcher(content)
                while (fenceMatcher.find()) {
                    val group = fenceMatcher.group(1)
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
            Rule("hr") { _: String, _: Node? ->
                "\n\n${options.hr}\n\n"
            },
        )
        addRule(
            "inlineLink",
            Rule(
                { node: Node -> options.linkStyle == LinkStyle.INLINED && node.nodeName() == "a" && node.attr("href").isNotEmpty() },
            ) { content: String, node: Node ->
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
                { node: Node -> options.linkStyle == LinkStyle.REFERENCED && node.nodeName() == "a" && node.attr("href").isNotEmpty() },
                { content: String, node: Node ->
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
            Rule(setOf("em", "i")) { content: String, _: Node ->
                if (content.trim { it <= ' ' }.isEmpty()) {
                    ""
                } else {
                    options.emDelimiter + content + options.emDelimiter
                }
            },
        )
        addRule(
            "strong",
            Rule(setOf("strong", "b")) { content: String, _: Node ->
                if (content.isBlank()) {
                    ""
                } else {
                    options.strongDelimiter + content + options.strongDelimiter
                }
            },
        )
        addRule(
            "code",
            Rule({ node: Node ->
                val hasSiblings = node.previousSibling() != null || node.nextSibling() != null
                val isCodeBlock = node.parentNode()!!.nodeName() == "pre" && !hasSiblings
                node.nodeName() == "code" && !isCodeBlock
            }, { content: String, _: Node ->
                if (content.trim { it <= ' ' }.isEmpty()) {
                    ""
                } else {
                    var delimiter = "`"
                    var leadingSpace = ""
                    var trailingSpace = ""
                    val pattern = Pattern.compile("(?m)(`)+")
                    val matcher = pattern.matcher(content)
                    if (matcher.find()) {
                        if (Pattern.compile("^`").matcher(content).find()) {
                            leadingSpace = " "
                        }
                        if (Pattern.compile("`$").matcher(content).find()) {
                            trailingSpace = " "
                        }
                        var counter = 1
                        if (delimiter == matcher.group()) {
                            counter++
                        }
                        while (matcher.find()) {
                            if (delimiter == matcher.group()) {
                                counter++
                            }
                        }
                        delimiter = java.lang.String.join("", Collections.nCopies(counter, "`"))
                    }
                    delimiter + leadingSpace + content + trailingSpace + delimiter
                }
            }),
        )
        addRule(
            "img",
            Rule("img") { _: String, node: Node ->
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
            Rule(setOf("del", "s", "strike")) { content: String, _: Node ->
                "~$content~"
            },
        )
        addRule(
            "taskListItems",
            Rule({ node: Node -> node.nodeName() == "input" && node.attr("type") == "checkbox" }) { _: String, node: Node ->
                if (node.hasAttr("checked")) {
                    "[x] "
                } else {
                    "[ ] "
                }
            },
        )

        addRule(
            "highlightedCodeBlock",
            Rule({ node: Node ->
                node.nodeName() == "div" &&
                    highlightRegExp.matcher(node.attr("class")).matches() &&
                    node.firstChild()?.nodeName() == "pre"
            }) { _: String, node: Node ->
                val className = node.attr("class") ?: ""
                val language = highlightRegExp.matcher(className).group(1)
                "\n\n\${options.fence}$language\n${node.firstChild()}\n${options.fence}\n\n"
            },
        )

        addRule("tableCell", Table.tableCell())
        addRule("tableRow", Table.tableRow())
        addRule("table", Table.table())
        addRule("tableSection", Table.tableSection())
        addRule("tableCaption", Table.tableCaption())

        addRule(
            "default",
            Rule({ true }) { content: String, element: Node ->
                if (ProcessNode.isBlock(element)) "\n\n$content\n\n" else content
            },
        )
    }

    fun findRule(node: Node): Rule? {
        for (rule in rules) {
            if (rule.filter.test(node)) {
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
        val highlightRegExp = Pattern.compile("highlight-(?:text|source)-([a-z0-9]+)")
    }
}
