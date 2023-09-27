package com.ismartcoding.lib.html2md

class Options(
    val br: String = "  ",
    val hr: String = "* * *",
    val emDelimiter: String = "_",
    val strongDelimiter: String = "**",
    val headingStyle: HeadingStyle = HeadingStyle.SETEXT,
    val bulletListMaker: String = "*",
    val codeBlockStyle: CodeBlockStyle = CodeBlockStyle.INDENTED,
    val linkStyle: LinkStyle = LinkStyle.INLINED,
    val linkReferenceStyle: LinkReferenceStyle = LinkReferenceStyle.DEFAULT,
    val fence: String = "```",
)
