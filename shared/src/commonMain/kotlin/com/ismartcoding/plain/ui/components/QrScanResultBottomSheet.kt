package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.page.scan.components.ScanResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QrScanResultBottomSheet(
    value: String,
    onDismiss: () -> Unit,
) {
    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.scan_result)) {
            CopyIconButton(text = value, clipLabel = stringResource(Res.string.scan_result))
        }
        TopSpace()
        ScanResult(text = value)
        BottomSpace()
    }
}