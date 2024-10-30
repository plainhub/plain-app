package com.ismartcoding.plain.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.helpers.BitmapHelper
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.mergeimages.CombineBitmapTools
import com.ismartcoding.plain.ui.theme.cardBackgroundActive
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaFolderGridItem(
    modifier: Modifier = Modifier,
    m: DMediaBucket,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bitmapResult = remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val bitmaps = mutableListOf<Bitmap>()
            m.topItems.forEach { path ->
                val bm = BitmapHelper.decodeBitmapFromFileAsync(context, path, 200, 200)
                if (bm != null) {
                    bitmaps.add(bm)
                }
            }
            try {
                val softwareBitmaps = mutableListOf<Bitmap>()
                for (bitmap in bitmaps) {
                    // Convert hardware bitmap to software bitmap
                    val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    softwareBitmaps.add(softwareBitmap)
                }
                bitmapResult.value = CombineBitmapTools.combineBitmap(
                    200,
                    200,
                    softwareBitmaps,
                )
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) MaterialTheme.colorScheme.cardBackgroundActive else Color.Transparent)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = bitmapResult.value,
                    contentDescription = m.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            VerticalSpace(12.dp)

            Text(
                text = m.name,
                style = MaterialTheme.typography.listItemTitle(),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            VerticalSpace(4.dp)
            var subtitle = pluralStringResource(R.plurals.items, m.itemCount, m.itemCount)
            if (m.size > 0) {
                subtitle += " • " + m.size.formatBytes()
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.listItemSubtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}