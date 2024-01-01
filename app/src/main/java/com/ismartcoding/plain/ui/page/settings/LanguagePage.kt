package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.Language
import com.ismartcoding.plain.data.preference.LanguagePreference
import com.ismartcoding.plain.data.preference.LocalLocale
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePage(navController: NavHostController) {
    val context = LocalContext.current
    val language = LocalLocale.current
    val scope = rememberCoroutineScope()
    val list = mutableListOf<Locale?>()
    list.add(null)
    list.addAll(Language.locales)

    PScaffold(
        navController,
        topBarTitle = stringResource(R.string.language),
        content = {
            LazyColumn {
                item {
                    VerticalSpace(dp = 16.dp)
                    list.forEach {
                        PListItem(
                            title = it?.getDisplayName(it) ?: stringResource(id = R.string.use_device_language),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    LanguagePreference.putAsync(context, it)
                                }
                            },
                        ) {
                            RadioButton(selected = (it == null && language == null) || (it?.language == language?.language && it?.country == language?.country), onClick = {
                                scope.launch(Dispatchers.IO) {
                                    LanguagePreference.putAsync(context, it)
                                }
                            })
                        }
                    }
                    BottomSpace()
                }
            }
        },
    )
}
