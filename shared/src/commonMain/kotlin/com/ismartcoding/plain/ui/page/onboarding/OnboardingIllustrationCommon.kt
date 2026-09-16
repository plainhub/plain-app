package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.preferences.LocalDarkTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// The dark surface ramp is compressed, so containers tuned for light need a
// visibly lifted dark equivalent to keep the illustrations readable.
@Composable
internal fun isDarkTheme() = DarkTheme.isDarkTheme(LocalDarkTheme.current)

@Composable
internal fun loopProgress(durationMillis: Int): State<Float> {
    val transition = rememberInfiniteTransition(label = "onboarding")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "progress",
    )
}

internal fun ease(fraction: Float): Float = FastOutSlowInEasing.transform(fraction.coerceIn(0f, 1f))

@Composable
internal fun bezelColor(): Color =
    if (isDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

// Generated scenic thumbnails (composeResources/drawable/demo_photo_*.png) —
// real-looking demo photos, never dot placeholders.
internal fun demoPhotoRes(index: Int): DrawableResource = when (((index % 6) + 6) % 6) {
    0 -> Res.drawable.demo_photo_1
    1 -> Res.drawable.demo_photo_2
    2 -> Res.drawable.demo_photo_3
    3 -> Res.drawable.demo_photo_4
    4 -> Res.drawable.demo_photo_5
    else -> Res.drawable.demo_photo_6
}

@Composable
internal fun DemoPhoto(index: Int, modifier: Modifier = Modifier, cornerRadius: Dp = 6.dp) {
    Image(
        painter = painterResource(demoPhotoRes(index)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
    )
}
