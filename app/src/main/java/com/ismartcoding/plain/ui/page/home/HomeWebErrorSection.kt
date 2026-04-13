package com.ismartcoding.plain.ui.page.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PIconTextButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun HomeWebErrorSection(
    context: Context,
    navController: NavHostController,
    errorMessage: String,
    onRestartFix: () -> Unit,
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.cardBackgroundNormal,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.home_web_easy_failed_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold, lineHeight = 36.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                VerticalSpace(12.dp)
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalSpace(24.dp)
                Button(
                    onClick = onRestartFix,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                    ),
                ) {

                    Text(
                        text = stringResource(R.string.relaunch_app),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            POutlinedButton(stringResource(R.string.troubleshoot), onClick = { WebHelper.open(context, "https://plainapp.app/troubleshooting") })
            PIconTextButton(R.drawable.settings, stringResource(R.string.web_settings)) {
                navController.navigate(Routing.WebSettings)
            }
        }
    }
}

