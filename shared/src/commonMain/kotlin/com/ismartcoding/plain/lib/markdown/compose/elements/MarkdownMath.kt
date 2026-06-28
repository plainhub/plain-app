package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.LatexThemeColors

/**
 * Renders a LaTeX expression via the `huarangmeng/latex` Compose renderer.
 *
 * `Latex` itself doesn't have a display/inline mode switch — it auto-detects
 * `$$…$$` and `$…$` syntax inside the source. We keep our callers explicit:
 *  - `BLOCK_MATH`  → strip the surrounding `$$…$$` and pass the body.
 *  - `INLINE_MATH` → strip the surrounding `$…$` and pass the body.
 *
 * Display-style math is horizontally centred inside the parent so a lone
 * `$$…$$` equation sits in the middle of the line, matching the KaTeX /
 * MathJax behaviour web users are used to. Inline math keeps its
 * `Modifier` from the caller so the surrounding text baseline alignment
 * is preserved.
 *
 * `Latex` already reports parse errors as inline text when it cannot render
 * a formula, so we don't need a separate fallback here.
 */
@Composable
fun MarkdownMath(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    textColor: Color = Color.Unspecified,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {
    val config = LatexConfig(
        fontSize = fontSize,
        theme = if (textColor == Color.Unspecified) {
            LatexTheme.auto()
        } else {
            LatexTheme.auto(
                light = LatexThemeColors(color = textColor),
                dark = LatexThemeColors(color = textColor),
            )
        },
    )
    val isDisplay = latex.startsWith("$$")
    if (isDisplay) {
        // `Latex` always paints its canvas at the formula's natural width
        // (it ignores parent width constraints), so wrapping it in a
        // `Box(fillMaxWidth, contentAlignment = Center)` is what actually
        // puts a display equation in the middle of the line — the box
        // reserves the full container width and the `Latex` composable
        // sits at its natural width inside the centred alignment.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp)
                .padding(vertical = 4.dp)
                .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Latex(
                latex = latex,
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.CenterHorizontally)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                config = config,
                isDarkTheme = isDarkTheme,
            )
        }
    } else {
        Latex(
            latex = latex,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp)
                .padding(vertical = 4.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            config = config,
            isDarkTheme = isDarkTheme,
        )
    }
}
