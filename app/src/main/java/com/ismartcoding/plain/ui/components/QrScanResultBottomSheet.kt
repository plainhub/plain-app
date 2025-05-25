package com.ismartcoding.plain.ui.components

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.page.scan.components.ScanResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QrScanResultBottomSheet(
    context: android.content.Context,
    value: String,
    onDismiss: () -> Unit,
) {
    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        PBottomSheetTopAppBar(title = stringResource(id = R.string.scan_result)) {
            PIconButton(
                icon = R.drawable.copy,
                contentDescription = stringResource(android.R.string.copy),
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            ) {
                val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.scan_result), value)
                clipboardManager.setPrimaryClip(clip)
                DialogHelper.showTextCopiedMessage(value)
            }
        }
        TopSpace()
        ScanResult(context, text = value)
        BottomSpace()
    }
}