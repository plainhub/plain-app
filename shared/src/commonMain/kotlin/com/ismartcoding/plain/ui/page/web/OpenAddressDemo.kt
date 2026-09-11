package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource

/**
 * Compose-drawn "video" for Quick start step 2: a browser address bar where
 * the phone's real current URL is typed in, ending on the web login page.
 * The URL is built live from TempData (same source as the address bar above),
 * so the video always shows the address this device actually serves.
 * Steps: 0 typing into the address bar, 1 login page.
 */

private const val DEMO_TYPING_MS = 2800L

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
        contentMinHeight = 280.dp,
        contentBackground = DemoChromeBg,
    ) { step, elapsedMs ->
        val typedLen = (elapsedMs * url.length / DEMO_TYPING_MS).toInt().coerceIn(0, url.length)
        DemoBrowserFrame(
            url = if (step == 0) url.take(typedLen) else url,
            caret = step == 0,
            pageBackground = DemoLoginBg,
        ) {
            if (step >= 1) {
                DemoLoginPage()
            } else {
                // Browser default (new tab) page: logo circle + search bar.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 208.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
