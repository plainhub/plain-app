package com.ismartcoding.plain.ui.page.root.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PBlockButton
import com.ismartcoding.plain.ui.base.PWaveCircleIconButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddress
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing

@Composable
fun HomeWebSuccessSection(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.ready),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace(8.dp)
        PWaveCircleIconButton(
            icon = painterResource(R.drawable.power_settings),
            contentDescription = stringResource(R.string.stop),
            onClick = { mainVM.enableHttpServer(context, false) },
            buttonSize = 118.dp,
            iconSize = 56.dp,
            buttonColor = Color(0xFF1F8F63),
            iconTint = Color.White,
            waveColor = Color(0xFF7DE8C2),
        )
        VerticalSpace(18.dp)
        Text(
            text = stringResource(R.string.web_address_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        WebAddress(context = context, mainVM = mainVM)
        VerticalSpace(24.dp)
        PBlockButton(
            text = stringResource(R.string.permission_settings),
            onClick = { navController.navigate(Routing.WebSettings) }
        )
    }
}
