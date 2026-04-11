package com.ismartcoding.plain.ui.page.home

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.greenDot
import com.ismartcoding.plain.ui.theme.greenPill
import com.ismartcoding.plain.ui.theme.greenText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddress
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.web.HttpServerManager

@Composable
fun HomeWebSuccessSection(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
    showSettingsButton: Boolean = true,
    showIpAddresses: Boolean = false,
) {
    val onlineCount by HttpServerManager.wsSessionCount.collectAsState()

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
                    text = stringResource(R.string.web_portal_running),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold, lineHeight = 36.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                VerticalSpace(12.dp)
                Text(
                    text = stringResource(R.string.web_portal_desc_running),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalSpace(24.dp)
                Button(
                    onClick = { mainVM.enableHttpServer(context, false) },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.red,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.stop_service),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                if (onlineCount > 0) {
                    VerticalSpace(16.dp)
                    OnlineSessionsIndicator(count = onlineCount, onClick = { navController.navigate(Routing.Sessions) })
                }
            }
        }
        VerticalSpace(16.dp)
        HomeWebAddressSection(context, navController, mainVM, showSettingsButton = showSettingsButton, showIpAddresses = showIpAddresses)
    }
}

@Composable
private fun OnlineSessionsIndicator(count: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    // Two rings staggered by half the period for continuous ripple effect
    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1",
    )
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, 900, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2",
    )

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.greenPill,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dotColor = MaterialTheme.colorScheme.greenDot
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val center = this.center
                    // Expanding ripple rings
                    listOf(progress1, progress2).forEach { p ->
                        drawCircle(
                            color = dotColor.copy(alpha = (1f - p) * 0.5f),
                            radius = (size.minDimension / 2f) * p,
                            center = center,
                        )
                    }
                    // Solid core dot
                    drawCircle(color = dotColor, radius = size.minDimension * 0.22f, center = center)
                }
            }
            Text(
                text = stringResource(R.string.clients_online, count),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.greenText,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
