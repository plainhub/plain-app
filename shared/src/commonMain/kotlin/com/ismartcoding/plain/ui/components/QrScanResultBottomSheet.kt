package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.scanner.components.ScanResult
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QrScanResultBottomSheet(
    value: String,
    title: String,
    copyLabel: String,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    PModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        PBottomSheetTopAppBar(title = title) {
            IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                Icon(
                    painter = painterResource(UiRes.drawable.ui_drawable_copy),
                    contentDescription = copyLabel,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        TopSpace()
        ScanResult(text = value)
        BottomSpace()
    }
}