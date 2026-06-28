package com.ismartcoding.plain.lib.markdown.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownBlockQuote
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownBulletList
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownCheckBox
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownCodeBlock
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownCodeFence
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownDivider
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownHeader
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownImage
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownInlineImage
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownOrderedList
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownParagraph
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownTable
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownText
import com.ismartcoding.plain.lib.markdown.compose.elements.listDepth
import com.ismartcoding.plain.lib.markdown.model.MarkdownTypography
import com.ismartcoding.plain.lib.markdown.utils.getUnescapedTextInNode
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

typealias MarkdownComponent = @Composable (MarkdownComponentModel) -> Unit

typealias CustomMarkdownComponent = @Composable (IElementType, MarkdownComponentModel) -> Unit

/**
 * Model holding data relevant for a component
 */
@Stable
data class MarkdownComponentModel(
    val content: String,
    val node: ASTNode,
    val typography: MarkdownTypography,
    val extra: ImmutableMap<String, Any> = persistentMapOf(),
)

private fun MarkdownComponentModel.getUnescapedTextInNode() = node.getUnescapedTextInNode(content)

fun markdownComponents(
    text: MarkdownComponent = CurrentComponentsBridge.text,
    eol: MarkdownComponent = CurrentComponentsBridge.eol,
    codeFence: MarkdownComponent = CurrentComponentsBridge.codeFence,
    codeBlock: MarkdownComponent = CurrentComponentsBridge.codeBlock,
    heading1: MarkdownComponent = CurrentComponentsBridge.heading1,
    heading2: MarkdownComponent = CurrentComponentsBridge.heading2,
    heading3: MarkdownComponent = CurrentComponentsBridge.heading3,
    heading4: MarkdownComponent = CurrentComponentsBridge.heading4,
    heading5: MarkdownComponent = CurrentComponentsBridge.heading5,
    heading6: MarkdownComponent = CurrentComponentsBridge.heading6,
    setextHeading1: MarkdownComponent = CurrentComponentsBridge.setextHeading1,
    setextHeading2: MarkdownComponent = CurrentComponentsBridge.setextHeading2,
    blockQuote: MarkdownComponent = CurrentComponentsBridge.blockQuote,
    paragraph: MarkdownComponent = CurrentComponentsBridge.paragraph,
    orderedList: MarkdownComponent = CurrentComponentsBridge.orderedList,
    unorderedList: MarkdownComponent = CurrentComponentsBridge.unorderedList,
    image: MarkdownComponent = CurrentComponentsBridge.image,
    inlineImage: MarkdownComponent = CurrentComponentsBridge.inlineImage,
    horizontalRule: MarkdownComponent = CurrentComponentsBridge.horizontalRule,
    table: MarkdownComponent = CurrentComponentsBridge.table,
    checkbox: MarkdownComponent = CurrentComponentsBridge.checkbox,
    custom: CustomMarkdownComponent? = CurrentComponentsBridge.custom,
): MarkdownComponents = DefaultMarkdownComponents(
    text = text,
    eol = eol,
    codeFence = codeFence,
    codeBlock = codeBlock,
    heading1 = heading1,
    heading2 = heading2,
    heading3 = heading3,
    heading4 = heading4,
    heading5 = heading5,
    heading6 = heading6,
    setextHeading1 = setextHeading1,
    setextHeading2 = setextHeading2,
    blockQuote = blockQuote,
    paragraph = paragraph,
    orderedList = orderedList,
    unorderedList = unorderedList,
    image = image,
    inlineImage = inlineImage,
    horizontalRule = horizontalRule,
    table = table,
    checkbox = checkbox,
    custom = custom,
)

/**
 * Interface defining all supported components.
 */
@Immutable
interface MarkdownComponents {
    val text: MarkdownComponent
    val eol: MarkdownComponent
    val codeFence: MarkdownComponent
    val codeBlock: MarkdownComponent
    val heading1: MarkdownComponent
    val heading2: MarkdownComponent
    val heading3: MarkdownComponent
    val heading4: MarkdownComponent
    val heading5: MarkdownComponent
    val heading6: MarkdownComponent
    val setextHeading1: MarkdownComponent
    val setextHeading2: MarkdownComponent
    val blockQuote: MarkdownComponent
    val paragraph: MarkdownComponent
    val orderedList: MarkdownComponent
    val unorderedList: MarkdownComponent
    val image: MarkdownComponent
    val inlineImage: MarkdownComponent
    val horizontalRule: MarkdownComponent
    val table: MarkdownComponent
    val checkbox: MarkdownComponent
    val custom: CustomMarkdownComponent?
}

@Immutable
private data class DefaultMarkdownComponents(
    override val text: MarkdownComponent,
    override val eol: MarkdownComponent,
    override val codeFence: MarkdownComponent,
    override val codeBlock: MarkdownComponent,
    override val heading1: MarkdownComponent,
    override val heading2: MarkdownComponent,
    override val heading3: MarkdownComponent,
    override val heading4: MarkdownComponent,
    override val heading5: MarkdownComponent,
    override val heading6: MarkdownComponent,
    override val setextHeading1: MarkdownComponent,
    override val setextHeading2: MarkdownComponent,
    override val blockQuote: MarkdownComponent,
    override val paragraph: MarkdownComponent,
    override val orderedList: MarkdownComponent,
    override val unorderedList: MarkdownComponent,
    override val image: MarkdownComponent,
    override val inlineImage: MarkdownComponent,
    override val horizontalRule: MarkdownComponent,
    override val table: MarkdownComponent,
    override val checkbox: MarkdownComponent,
    override val custom: CustomMarkdownComponent?,
) : MarkdownComponents

/**
 * Adapts the universal signature @Composable (MarkdownComponentModel) -> Unit to the existing components.
 */
object CurrentComponentsBridge {
    val text: MarkdownComponent = {
        MarkdownText(it.getUnescapedTextInNode(), it.node, style = it.typography.text)
    }
    val eol: MarkdownComponent = { }
    val codeFence: MarkdownComponent = {
        MarkdownCodeFence(it.content, it.node, style = it.typography.code)
    }
    val codeBlock: MarkdownComponent = {
        MarkdownCodeBlock(it.content, it.node, style = it.typography.code)
    }
    val heading1: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h1)
    }
    val heading2: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h2)
    }
    val heading3: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h3)
    }
    val heading4: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h4)
    }
    val heading5: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h5)
    }
    val heading6: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h6)
    }
    val setextHeading1: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h1, contentChildType = MarkdownTokenTypes.SETEXT_CONTENT)
    }
    val setextHeading2: MarkdownComponent = {
        MarkdownHeader(it.content, it.node, style = it.typography.h2, contentChildType = MarkdownTokenTypes.SETEXT_CONTENT)
    }
    val blockQuote: MarkdownComponent = {
        MarkdownBlockQuote(it.content, it.node, style = it.typography.quote)
    }
    val paragraph: MarkdownComponent = {
        MarkdownParagraph(it.content, it.node, style = it.typography.paragraph)
    }
    val orderedList: MarkdownComponent = {
        MarkdownOrderedList(it.content, it.node, style = it.typography.ordered, it.listDepth)
    }
    val unorderedList: MarkdownComponent = {
        MarkdownBulletList(it.content, it.node, style = it.typography.bullet, it.listDepth)
    }
    val image: MarkdownComponent = {
        MarkdownImage(it.content, it.node)
    }
    val inlineImage: MarkdownComponent = {
        MarkdownInlineImage(it.content, it.node)
    }
    val horizontalRule: MarkdownComponent = {
        MarkdownDivider(Modifier.fillMaxWidth())
    }
    val table: MarkdownComponent = {
        MarkdownTable(it.content, it.node, style = it.typography.table)
    }
    val checkbox: MarkdownComponent = {
        MarkdownCheckBox(it.content, it.node, style = it.typography.text)
    }
    val custom: CustomMarkdownComponent? = null
}
