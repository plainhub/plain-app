package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey
import com.ismartcoding.plain.ui.theme.orange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The dark surface ramp is compressed, so containers tuned for light need a
// visibly lifted dark equivalent to keep the illustrations readable.
@Composable
private fun isDarkTheme() = DarkTheme.isDarkTheme(LocalDarkTheme.current)

@Composable
private fun loopProgress(durationMillis: Int): State<Float> {
    val transition = rememberInfiniteTransition(label = "onboarding")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress",
    )
}

private fun ease(fraction: Float): Float = FastOutSlowInEasing.transform(fraction.coerceIn(0f, 1f))

// Illustration "photo" placeholder: soft tinted tile with a dot, theme-aware.
@Composable
private fun DemoPhotoTile(
    index: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 7.dp,
    dotSize: Dp = 18.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val (bg, dot) = when (index % 6) {
        0 -> scheme.primary.copy(alpha = 0.15f) to scheme.primary
        1 -> scheme.orange.copy(alpha = 0.15f) to scheme.orange
        2 -> scheme.green.copy(alpha = 0.15f) to scheme.green
        3 -> scheme.tertiaryContainer to scheme.tertiary
        4 -> scheme.error.copy(alpha = 0.15f) to scheme.error
        else -> scheme.grey.copy(alpha = 0.15f) to scheme.grey
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dot.copy(alpha = 0.75f)),
        )
    }
}

@Composable
private fun DashedLine(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(2.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
            },
    )
}

@Composable
private fun DeviceLink(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.wifi),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "Wi-Fi",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        VerticalSpace(dp = 8.dp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(56.dp)) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            DashedLine(Modifier.weight(1f))
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

// ---------- Device frames ----------

/** Phone mockup frame (bezel + screen), screen background matches the app. */
@Composable
private fun PhoneFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .height(192.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(3.dp, bezelColor(), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

/** Desktop browser window: traffic lights + address bar, then page content. */
@Composable
private fun BrowserFrame(
    modifier: Modifier = Modifier,
    height: Dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, bezelColor(), RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(if (isDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.orange))
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.green))
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .weight(3f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDarkTheme()) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(8.dp),
                )
                Text(
                    text = "192.168.1.20",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
        }
        content()
    }
}

@Composable
private fun bezelColor(): Color =
    if (isDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

// ---------- Web UI (plain-desktop): icon rail + content ----------

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
private fun WebRail(activeIndex: Int, pressed: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        webSections.forEachIndexed { index, section ->
            val isActive = index == activeIndex
            Column(
                modifier = Modifier
                    .width(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isActive && pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                    )
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Icon(
                    painter = painterResource(section.icon),
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = stringResource(section.label),
                    fontSize = 7.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Content pane for the active web section — real app content, not placeholders. */
@Composable
private fun WebContent(sectionIndex: Int, modifier: Modifier = Modifier) {
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
                    DemoPhotoTile(
                        index = row * cols + col,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cornerRadius = 4.dp,
                        dotSize = 9.dp,
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
                        DemoPhotoTile(index = row * 2 + col, modifier = Modifier.fillMaxSize(), cornerRadius = 4.dp, dotSize = 10.dp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.play_arrow),
                                contentDescription = null,
                                tint = Color.White,
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
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(9.dp),
                    )
                }
                Text(text = "song_0${index + 1}.mp3", fontSize = 8.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Text(text = "3:4$index", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = time, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ---------- Slide 1: phone (app UI) + computer (web UI) ----------

/** App home screen on the phone: top bar, date group, photo grid, bottom nav. */
@Composable
private fun AppHomeScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(Res.string.home), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Icon(
                painter = painterResource(Res.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(10.dp),
            )
        }
        Text(
            text = stringResource(Res.string.today),
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(2) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { col ->
                        DemoPhotoTile(
                            index = row * 3 + col,
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            cornerRadius = 4.dp,
                            dotSize = 8.dp,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem(Res.drawable.house, Res.string.home, selected = true)
            BottomNavItem(Res.drawable.message_circle, Res.string.chat, selected = false)
            BottomNavItem(Res.drawable.grid_3x3, Res.string.tools, selected = false)
        }
    }
}

@Composable
private fun BottomNavItem(icon: DrawableResource, label: StringResource, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = stringResource(label),
            fontSize = 6.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun WelcomeIllustration() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PhoneFrame { AppHomeScreen() }
        DeviceLink(Modifier.padding(horizontal = 4.dp))
        BrowserFrame(modifier = Modifier.weight(1f), height = 136.dp) {
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

/** Share-folder message card, mirroring ChatShareItem. */
@Composable
private fun ShareCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.folders),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(7.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = "Documents",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.folder_card_items, 24) + " · 1.2 GB",
                    fontSize = 7.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Chat message row, mirroring ChatListItem: name + time, accent bar for me. */
@Composable
private fun ChatMessageRow(
    fromMe: Boolean,
    name: String,
    time: String,
    appear: Float,
    content: @Composable () -> Unit,
) {
    val a = ease(appear)
    if (a <= 0f) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(a)
            .graphicsLayer { translationY = (1f - a) * 10.dp.toPx() },
    ) {
        if (fromMe) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        } else {
            HorizontalSpace(dp = 4.dp)
        }
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 5.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = time,
                    fontSize = 7.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
fun ChatIllustration() {
    val p by loopProgress(8000)
    val fadeOut = 1f - ((p - 0.92f) / 0.08f).coerceIn(0f, 1f)
    val me = stringResource(Res.string.me)
    Column(
        modifier = Modifier
            .width(304.dp)
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
            modifier = Modifier.fillMaxWidth().alpha(fadeOut),
            verticalArrangement = Arrangement.spacedBy(3.dp),
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
                    DemoPhotoTile(0, Modifier.size(40.dp), cornerRadius = 6.dp, dotSize = 10.dp)
                    DemoPhotoTile(2, Modifier.size(40.dp), cornerRadius = 6.dp, dotSize = 10.dp)
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
        // Chat input: bordered field + send, mirroring ChatInput.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.chat_input_hint),
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(Res.drawable.send),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
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
                .background(if (hero) scheme.surface else scheme.primaryContainer),
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
