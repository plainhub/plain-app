package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper

/**
 * Compose-drawn "video" for Quick start step 2: a browser address bar where
 * the phone's real current URL is typed in, ending on the web login page.
 * The URL is built live from TempData (same source as the address bar above),
 * so the video always shows the address this device actually serves.
 * The login mock mirrors plain-desktop LoginView/LoginForm colors
 * (src/styles/_base.scss light theme).
 * Steps: 0 typing into the address bar, 1 login page.
 */

private const val DEMO_TYPING_MS = 2800L

// plain-desktop light theme (src/styles/_base.scss)
private val LoginBg = Color(0xFFFBF8FF)
private val LoginCard = Color(0xFFE2E0F7) // --md-sys-color-surface-variant
private val LoginPrimary = Color(0xFF3F51B5) // --md-sys-color-primary
private val LoginOnSurface = Color(0xFF1A1B26)
private val LoginOnSurfaceVariant = Color(0xFF454558)
private val LoginOutline = Color(0xFF757589)

@Composable
fun OpenAddressDemo() {
    val isHttps = TempData.webHttps.collectAsState()
    val port = if (isHttps.value) TempData.httpsPort.collectAsState() else TempData.httpPort.collectAsState()
    val url = remember(isHttps.value, port.value) {
        UrlHelper.buildUrl(if (isHttps.value) "https" else "http", demoServerIp(), port.value)
    }

    DemoPlayer(
        stepDurationsMs = listOf(DEMO_TYPING_MS),
        endHoldMs = 1400L,
        contentMinHeight = 248.dp,
        contentBackground = LoginBg,
    ) { step, elapsedMs ->
        val typedLen = (elapsedMs * url.length / DEMO_TYPING_MS).toInt().coerceIn(0, url.length)
        Column(modifier = Modifier.fillMaxWidth()) {
            DemoBrowserChrome(
                url = if (step == 0) url.take(typedLen) else url,
                caret = step == 0,
            )
            if (step >= 1) {
                DemoLoginPage()
            }
        }
    }
}

/** plain-desktop LoginView: PlainApp title + card with password field and Log in button. */
@Composable
private fun DemoLoginPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoginBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "PlainApp",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LoginOnSurface,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LoginCard)
                .padding(20.dp),
        ) {
            // Password field (outlined, on the surface-variant card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, LoginOutline.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelSmall,
                    color = LoginOnSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(LoginPrimary)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Log in",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
