package com.ismartcoding.plain.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.ismartcoding.lib.helpers.BitmapHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.ui.extensions.navigateImages
import com.ismartcoding.plain.ui.views.mergeimages.CombineBitmapTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun MediaBucketGridItem(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    m: DMediaBucket,
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
            .clickable {
                navController.navigateImages(m.id)
            },
    ) {
        GlideImage(
            model = bitmapResult.value,
            contentDescription = m.name,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
            transition = CrossFade,
            failure = placeholder(R.drawable.ic_broken_image),
        )
        Box(
            modifier =
            Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Text(
                modifier =
                Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                text = m.name + " (${m.itemCount})",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}