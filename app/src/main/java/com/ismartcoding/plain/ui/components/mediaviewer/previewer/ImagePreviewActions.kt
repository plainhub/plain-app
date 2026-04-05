package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.content.Context
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.theme.darkMask
import com.ismartcoding.plain.ui.theme.lightMask
import kotlinx.coroutines.launch

@Composable
fun ImagePreviewActions(
    context: Context, castViewModel: CastViewModel,
    m: PreviewItem, state: MediaPreviewerState,
) {
    val scope = rememberCoroutineScope()

    CastDialog(castViewModel)

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .navigationBarsPadding()
            .alpha(state.uiAlpha.value),
    ) {
        if (!state.showActions) return
        if (castViewModel.castMode.value) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.darkMask())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                POutlinedButton(text = stringResource(id = R.string.cast), small = true, onClick = { castViewModel.cast(m.path) })
                HorizontalSpace(dp = 20.dp)
                POutlinedButton(text = stringResource(id = R.string.exit_cast_mode), small = true, contentColor = Color.LightGray, onClick = { castViewModel.exitCastMode() })
            }
            return
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.darkMask())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ActionIconButton(icon = R.drawable.share_2, contentDescription = stringResource(R.string.share)) {
                scope.launch { sharePreviewImage(context, m) }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(icon = R.drawable.cast, contentDescription = stringResource(R.string.cast)) {
                castViewModel.showCastDialog.value = true
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(icon = R.drawable.rotate_cw_square, contentDescription = stringResource(R.string.rotate)) {
                scope.launch {
                    state.viewerContainerState?.viewerState?.let {
                        it.rotation.animateTo(it.rotation.value + 90, SpringSpec())
                    }
                }
            }
            if (m.data !is DImage && m.data !is DFile) {
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(icon = R.drawable.save, contentDescription = stringResource(R.string.save)) {
                    scope.launch { savePreviewImage(context, m) }
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(icon = R.drawable.ellipsis, contentDescription = stringResource(R.string.more_info)) {
                state.showMediaInfo = true
            }
        }
    }
}

@Composable
fun ActionIconButton(icon: Int, contentDescription: String, click: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.lightMask())
            .clickable { click() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}
