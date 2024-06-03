package com.ismartcoding.plain.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.helpers.BitmapHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nav.navigateImages
import com.ismartcoding.plain.ui.nav.navigateVideos
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.views.mergeimages.CombineBitmapTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaBucketGridItem(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    m: DMediaBucket,
    dataType: DataType
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
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                if (dataType == DataType.IMAGE) {
                    navController.navigateImages(m.id)
                } else if (dataType == DataType.VIDEO) {
                    navController.navigateVideos(m.id)
                }
            },
    ) {
        Column(
            modifier = modifier
                .padding(8.dp)
        ) {
            AsyncImage(
                model = bitmapResult.value,
                contentDescription = m.name,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            VerticalSpace(dp = 8.dp)
            Text(
                modifier =
                Modifier
                    .padding(horizontal = 4.dp),
                text = m.name,
                style = MaterialTheme.typography.listItemTitle(),
            )
            VerticalSpace(dp = 8.dp)
            Text(
                modifier =
                Modifier
                    .padding(horizontal = 4.dp),
                text = pluralStringResource(R.plurals.items, m.itemCount, m.itemCount) + " • " + m.size.formatBytes(),
                style = MaterialTheme.typography.listItemSubtitle(),
            )
        }
    }
}