package com.ismartcoding.plain.ui.page.files.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun FilePasteBar(
    filesVM: FilesViewModel,
    coroutineScope: CoroutineScope,
    onPasteComplete: () -> Unit
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                filesVM.cutFiles.clear()
                filesVM.copyFiles.clear()
                filesVM.showPasteBar.value = false
            }) {
                Icon(painter = painterResource(R.drawable.x), contentDescription = "Cancel")
            }

            Text(
                text = if (filesVM.cutFiles.isNotEmpty())
                    LocaleHelper.getQuantityString(R.plurals.moving_items, filesVM.cutFiles.size)
                else
                    LocaleHelper.getQuantityString(R.plurals.copying_items, filesVM.copyFiles.size),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(onClick = {
                coroutineScope.launch {
                    if (filesVM.cutFiles.isNotEmpty()) {
                        executeCutFiles(filesVM, onPasteComplete)
                    } else if (filesVM.copyFiles.isNotEmpty()) {
                        executeCopyFiles(filesVM, onPasteComplete)
                    }
                }
            }) {
                Text(stringResource(R.string.paste))
            }
        }
    }
} 