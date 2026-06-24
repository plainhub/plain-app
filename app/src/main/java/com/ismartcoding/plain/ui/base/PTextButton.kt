package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.enums.ButtonSize

@Composable
fun PTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ButtonSize = ButtonSize.MEDIUM,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(buttonSize.height),
        shape = RoundedCornerShape(buttonSize.cornerRadius),
    ) {
        Text(
            text = text,
            style = buttonSize.textStyle(),
            fontWeight = buttonSize.fontWeight(),
        )
    }
}
