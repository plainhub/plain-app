package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.preferences.LanguagePreference
import com.ismartcoding.plain.preferences.LocalLocale
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.PlainTheme
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
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.language))
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                itemsIndexed(list) { index, item ->
                    PListItem(
                        modifier = PlainTheme
                            .getCardModifier(index = if (index > 0) index - 1 else 0, size = if (index > 0) list.size - 1 else 1)
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    LanguagePreference.putAsync(context, item)
                                }
                            },
                        title = item?.getDisplayName(item) ?: stringResource(id = R.string.use_device_language),
                    ) {
                        RadioButton(selected = (item == null && language == null) || (item?.language == language?.language && item?.country == language?.country), onClick = {
                            scope.launch(Dispatchers.IO) {
                                LanguagePreference.putAsync(context, item)
                            }
                        })
                    }
                    if (index == 0) {
                        VerticalSpace(16.dp)
                    }
                }
                item {
                    BottomSpace(paddingValues)
                }
            }
        },
    )
}
