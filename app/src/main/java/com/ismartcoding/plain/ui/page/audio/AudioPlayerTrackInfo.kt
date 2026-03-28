package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerTrackInfo(title: String, artist: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val titleScrollState = rememberScrollState()
        val isScrollingTitle = remember { mutableStateOf(false) }

        LaunchedEffect(title) {
            delay(1500)
            if (titleScrollState.maxValue > 0) {
                isScrollingTitle.value = true
                while (isScrollingTitle.value) {
                    titleScrollState.animateScrollTo(0)
                    delay(2000)
                    titleScrollState.animateScrollTo(titleScrollState.maxValue)
                    delay(2000)
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Visible,
                modifier = Modifier.horizontalScroll(titleScrollState, reverseScrolling = false),
            )
        }

        VerticalSpace(8.dp)

        val artistScrollState = rememberScrollState()
        val isScrollingArtist = remember { mutableStateOf(false) }

        LaunchedEffect(artist) {
            delay(1500)
            if (artistScrollState.maxValue > 0) {
                isScrollingArtist.value = true
                while (isScrollingArtist.value) {
                    artistScrollState.animateScrollTo(0)
                    delay(2000)
                    artistScrollState.animateScrollTo(artistScrollState.maxValue)
                    delay(2000)
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Visible,
                modifier = Modifier.horizontalScroll(artistScrollState, reverseScrolling = false),
            )
        }
    }
}
