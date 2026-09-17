package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeIllustration() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Phone and browser share one height; below ~185dp the phone mock
        // clips its bottom nav. Content lays out natively at the frame size.
        PhoneFrame(modifier = Modifier.width(104.dp).height(200.dp)) { AppHomeScreen() }
        DeviceLink(Modifier.padding(horizontal = 2.dp))
        BrowserFrame(modifier = Modifier.weight(1f), height = 200.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                WebRail(activeIndex = 2, pressed = false)
                WebContent(sectionIndex = 2, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------- Slide 2: browser cycling Images → Videos → Audio → Chat ----------

@Composable
fun DesktopAccessIllustration() {
    val p by loopProgress(7200)
    // Rail order is Files, Audios, Images, Videos, Chat — cycle the media
    // sections in the user-visible order: Images, Videos, Audio, Chat.
    val activeCycle = listOf(2, 3, 1, 4)
    val phase = (p * 4f).toInt().coerceIn(0, 3)
    val local = p * 4f - phase
    val pressed = local < 0.18f
    BrowserFrame(modifier = Modifier.width(304.dp), height = 212.dp) {
        Row(modifier = Modifier.fillMaxSize()) {
            WebRail(activeIndex = activeCycle[phase], pressed = pressed)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                key(phase) {
                    WebContent(
                        sectionIndex = activeCycle[phase],
                        modifier = Modifier.alpha(ease(local / 0.15f)),
                    )
                }
            }
        }
    }
}

// ---------- Slide 3: app chat, animated conversation ----------

@Composable
fun ChatIllustration() {
    val p by loopProgress(8000)
    val fadeOut = 1f - ((p - 0.92f) / 0.08f).coerceIn(0f, 1f)
    val me = stringResource(Res.string.me)
    Column(
        modifier = Modifier
            .width(304.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, bezelColor(), RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(Res.string.chat), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(
                painter = painterResource(Res.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
        }
        VerticalSpace(dp = 4.dp)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().alpha(fadeOut),
            verticalArrangement = Arrangement.spacedBy(3.dp, alignment = Alignment.Bottom),
        ) {
            // Timeline: peer text → my text → my images → my shared folder → my text.
            ChatMessageRow(fromMe = false, name = "MacBook", time = "09:41", appear = (p - 0.02f) / 0.06f) {
                Text(
                    text = stringResource(Res.string.onboarding_3_bubble_in),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
            ChatMessageRow(fromMe = true, name = me, time = "09:42", appear = (p - 0.15f) / 0.06f) {
                Text(
                    text = stringResource(Res.string.onboarding_3_bubble_out),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
            ChatMessageRow(fromMe = true, name = me, time = "09:43", appear = (p - 0.28f) / 0.06f) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                    DemoPhoto(0, Modifier.size(40.dp), cornerRadius = 6.dp)
                    DemoPhoto(3, Modifier.size(40.dp), cornerRadius = 6.dp)
                }
            }
            ChatMessageRow(fromMe = true, name = me, time = "09:44", appear = (p - 0.42f) / 0.06f) {
                ShareCard()
            }
            ChatMessageRow(fromMe = true, name = me, time = "09:45", appear = (p - 0.56f) / 0.06f) {
                Text(
                    text = stringResource(Res.string.onboarding_3_msg_sent),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
        VerticalSpace(dp = 4.dp)
        // Chat input: bordered field with the send icon inside (bottom-right),
        // mirroring ChatInput.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(start = 8.dp, end = 5.dp, top = 4.dp, bottom = 3.dp),
        ) {
            Text(
                text = stringResource(Res.string.chat_input_hint),
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Icon(
                painter = painterResource(Res.drawable.send),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterEnd).size(11.dp),
            )
        }
    }
}

// ---------- Slide 4: tools grid ----------

@Composable
private fun ToolCard(
    modifier: Modifier,
    icon: DrawableResource,
    title: String,
    subtitle: String,
    hero: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (hero) scheme.primaryContainer else scheme.cardBackgroundNormal)
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                // Default cover pairing (primaryContainer→tertiaryContainer
                // gradient): the dark surface circle read as a black hole.
                .background(
                    Brush.linearGradient(
                        listOf(scheme.tertiaryContainer, scheme.primaryContainer),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        VerticalSpace(dp = 6.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        VerticalSpace(dp = 4.dp)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = if (hero) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            fontWeight = if (hero) FontWeight.SemiBold else null,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MediaToolsIllustration() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.image,
                title = stringResource(Res.string.onboarding_4_photo_title),
                subtitle = stringResource(Res.string.onboarding_4_photo_desc),
                hero = false,
            )
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.music2,
                title = stringResource(Res.string.onboarding_4_music_title),
                subtitle = stringResource(Res.string.onboarding_4_music_desc),
                hero = true,
            )
        }
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.notebook_pen,
                title = stringResource(Res.string.notes),
                subtitle = stringResource(Res.string.onboarding_4_notes_desc),
                hero = false,
            )
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.scan_qr_code,
                title = stringResource(Res.string.tools),
                subtitle = stringResource(Res.string.onboarding_4_tools_desc),
                hero = false,
            )
        }
    }
}
