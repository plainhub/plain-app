package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Mini default track cover, mirroring the app's DefaultCoverArt pairing. */
@Composable
private fun MiniMusicCover(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

private data class WebSection(val icon: DrawableResource, val label: StringResource)

private val webSections: List<WebSection>
    get() = listOf(
        WebSection(Res.drawable.folder, Res.string.files),
        WebSection(Res.drawable.music, Res.string.audios),
        WebSection(Res.drawable.image, Res.string.images),
        WebSection(Res.drawable.video, Res.string.videos),
        WebSection(Res.drawable.message_circle, Res.string.chat),
    )

/** Icon rail mirroring plain-desktop app-rail: icon over label, primary pill on active. */
@Composable
internal fun WebRail(activeIndex: Int, pressed: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(38.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        webSections.forEachIndexed { index, section ->
            val isActive = index == activeIndex
            Column(
                modifier = Modifier
                    .width(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isActive && pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                    )
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Icon(
                    painter = painterResource(section.icon),
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else mockupMutedColor(),
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = stringResource(section.label),
                    fontSize = 7.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else mockupMutedColor(),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Content pane for the active web section — real app content, not placeholders. */
@Composable
internal fun WebContent(sectionIndex: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(6.dp)) {
        when (sectionIndex) {
            2 -> WebMediaGrid(rows = 3, cols = 3)
            3 -> WebVideoGrid()
            1 -> WebAudioList()
            else -> WebChatPreview()
        }
    }
}

@Composable
private fun WebMediaGrid(rows: Int, cols: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(cols) { col ->
                    DemoPhoto(
                        index = row * cols + col,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cornerRadius = 4.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WebVideoGrid() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(2) { col ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp)),
                    ) {
                        DemoPhoto(index = row * 2 + col + 2, modifier = Modifier.fillMaxSize(), cornerRadius = 4.dp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.play_arrow),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(9.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebAudioList() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(4) { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MiniMusicCover(size = 16.dp)
                Text(text = "song_0${index + 1}.mp3", fontSize = 8.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Text(text = "3:4$index", fontSize = 7.sp, color = mockupMutedColor())
            }
        }
    }
}

/** One chat message row, mirroring the app ChatListItem layout. */
@Composable
private fun MiniChatRow(fromMe: Boolean, name: String, time: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        if (fromMe) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        } else {
            HorizontalSpace(dp = 3.dp)
        }
        Column(modifier = Modifier.padding(start = 6.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = name, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = time, fontSize = 7.sp, color = mockupMutedColor())
            }
            Text(
                text = text,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WebChatPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        MiniChatRow(fromMe = false, name = "MacBook", time = "09:41", text = stringResource(Res.string.onboarding_3_bubble_in))
        MiniChatRow(fromMe = true, name = stringResource(Res.string.me), time = "09:42", text = stringResource(Res.string.onboarding_3_bubble_out))
        MiniChatRow(fromMe = true, name = stringResource(Res.string.me), time = "09:43", text = stringResource(Res.string.onboarding_3_msg_sent))
    }
}
