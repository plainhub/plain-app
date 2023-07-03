package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.Language
import com.ismartcoding.plain.data.preference.LanguagePreference
import com.ismartcoding.plain.data.preference.LocalLanguage
import com.ismartcoding.plain.ui.base.BottomSpacer
import com.ismartcoding.plain.ui.base.DisplayText
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()

    PScaffold(
        navController,
        content = {
            LazyColumn {
                item(key = language) {
                    DisplayText(text = stringResource(R.string.language))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Language.values().map {
                        PListItem(
                            title = it.getText(context),
                            onClick = {
                                LanguagePreference.put(context, scope, it)
                            },
                        ) {
                            RadioButton(selected = it.value == language, onClick = {
                                LanguagePreference.put(context, scope, it)
                            })
                        }
                    }
                    BottomSpacer()
                }
            }
        }
    )
}
