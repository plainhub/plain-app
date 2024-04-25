package com.ismartcoding.plain.ui.page


import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.base.PBlockButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.openPathIntent
import java.io.File

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherFilePage(
    navController: NavHostController,
    path: String
) {
    val context = LocalContext.current
    val file = File(path)

    PScaffold(
        navController,
        actions = {
            PIconButton(
                icon = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                ShareHelper.shareFile(context, File(path))
            }
        },
        content = {
            LazyColumn {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            modifier =
                            Modifier
                                .padding(bottom = 32.dp)
                                .size(56.dp),
                            painter = painterResource(id = R.drawable.ic_unknown_file),
                            contentDescription = "",
                        )
                        SelectionContainer {
                            Text(
                                text = file.name,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                        SelectionContainer {
                            Text(
                                text = stringResource(R.string.file_size) + ": " + FormatHelper.formatBytes(file.length()),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                        Text(
                            text = stringResource(R.string.unknown_file_description),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        VerticalSpace(dp = 64.dp)
                        PBlockButton(text = stringResource(id = R.string.open_with_other_app)) {
                            MainActivity.instance.get()?.openPathIntent(path)
                        }
                    }
                }
            }
        },
    )
}
