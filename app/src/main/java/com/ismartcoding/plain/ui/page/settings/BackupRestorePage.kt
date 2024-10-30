package com.ismartcoding.plain.ui.page.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatName
import com.ismartcoding.plain.ui.base.PCircularButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.helpers.FilePickHelper
import com.ismartcoding.plain.ui.models.BackupRestoreViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestorePage(
    navController: NavHostController,
    backupRestoreVM: BackupRestoreViewModel = viewModel(),
) {
    val context = LocalContext.current
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data?.data != null) {
                    backupRestoreVM.backup(context, data.data!!)
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                FilePickHelper.getUris(result.data!!).firstOrNull()?.let {
                    backupRestoreVM.restore(context, it)
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
                            exportLauncher.launch(
                                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    type = "text/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    putExtra(Intent.EXTRA_TITLE, "backup_" + Date().formatName() + ".zip")
                                },
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    PCircularButton(
                        text = stringResource(R.string.restore),
                        icon = R.drawable.archive_restore,
                        description = stringResource(R.string.restore),
                        onClick = {
                            restoreLauncher.launch(FilePickHelper.getPickFileIntent(false))
                        }
                    )
                }
            }
        }
    )
}
