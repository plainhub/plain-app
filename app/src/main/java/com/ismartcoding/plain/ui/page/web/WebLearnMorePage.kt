package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLearnMorePage(navController: NavHostController) {
    WebSettingsProvider {
        PScaffold(
            navController,
            topBarTitle = stringResource(id = R.string.web_console),
            content = {
                LazyColumn {
                    item {
                        TopSpace()
                        Subtitle(text = stringResource(id = R.string.instruction_for_use))
                        PCard {
                            Text(
                                stringResource(id = R.string.web_how_to),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item{
                        Subtitle(text = stringResource(id = R.string.recommendation))
                        PCard {
                            Text(
                                stringResource(id = R.string.usb_connect_recommendation),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        Subtitle(text = stringResource(id = R.string.troubleshoot))
                        PCard {
                            Text(
                                stringResource(id = R.string.web_dig),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        BottomSpace()
                    }
                }
            },
        )
    }
}
