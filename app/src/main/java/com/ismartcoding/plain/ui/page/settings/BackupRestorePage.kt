package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.isP
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.ExportFileResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.extensions.formatName
import com.ismartcoding.plain.ui.base.PCircularButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.models.BackupRestoreViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestorePage(
    navController: NavHostController,
    backupRestoreVM: BackupRestoreViewModel = viewModel(),
) {
    val context = LocalContext.current
    val sharedFlow = Channel.sharedFlow
    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.RESTORE) return@collect
                    event.uris.firstOrNull()?.let {
                        backupRestoreVM.restore(context, it)
                    }
                }
                is ExportFileResultEvent -> {
                    if (event.type != ExportFileType.BACKUP) return@collect
                    backupRestoreVM.backup(context, event.uri)
                }
            }
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.backup_restore))
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.backup_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 48.dp, bottom = 48.dp)
                    )

                    PCircularButton(
                        text = stringResource(R.string.backup),
                        icon = R.drawable.database_backup,
                        description = stringResource(R.string.backup),
                        onClick = {
                            val fileName = "backup_" + Date().formatName() + ".zip"
                            // ACTION_CREATE_DOCUMENT is broken on many Samsung Android 9 devices:
                            // it returns RESULT_CANCELED with no URI even after the user picks a location.
                            // On Android 9 and below, skip the file picker and write directly to
                            // app-specific external storage instead.
                            if (isP()) {
                                backupRestoreVM.backupToFile(context, fileName)
                            } else {
                                sendEvent(ExportFileEvent(ExportFileType.BACKUP, fileName))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    PCircularButton(
                        text = stringResource(R.string.restore),
                        icon = R.drawable.archive_restore,
                        description = stringResource(R.string.restore),
                        onClick = {
                            sendEvent(PickFileEvent(PickFileTag.RESTORE, PickFileType.FILE, false))
                        }
                    )
                }
            }
        }
    )
}
