package com.ismartcoding.plain.ui.page.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.ui.base.PIconTextButton
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.HttpHttpsSegmentedButton
import com.ismartcoding.plain.ui.components.WebAddressBar
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.launch

@Composable
fun HomeWebAddressSection(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
) {
    var isHttps by remember { mutableStateOf(TempData.webHttps) }
    val scope = rememberCoroutineScope()

    Column {
        Text(
            text = stringResource(R.string.web_address_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        VerticalSpace(12.dp)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            WebAddressBar(context = context, mainVM = mainVM, isHttps = isHttps)
            VerticalSpace(12.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HttpHttpsSegmentedButton(
                    isHttps = isHttps,
                    onSelect = { https ->
                        isHttps = https
                        scope.launch { HttpsPreference.putAsync(context, https) }
                    },
                )
                    PIconTextButton(R.drawable.settings, stringResource(R.string.web_settings)) {
                        navController.navigate(Routing.WebSettings)
                    }
            }
        }
        VerticalSpace(8.dp)
        Tips(text = stringResource(R.string.same_network_hint))
    }
}
