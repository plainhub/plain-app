package com.ismartcoding.plain.ui.page

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImage
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageEventListener
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageSource
import com.ismartcoding.plain.ui.base.subsampling.ImageSourceProvider
import com.ismartcoding.plain.ui.base.subsampling.ScrollableContainerDirection
import com.ismartcoding.plain.ui.base.subsampling.helpers.asLog
import com.ismartcoding.plain.ui.base.subsampling.rememberComposeSubsamplingScaleImageState
import com.ismartcoding.plain.ui.models.SharedViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewPage(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.context as Activity).window
    // https://developer.android.com/develop/ui/views/layout/edge-to-edge
    val insetsController = WindowCompat.getInsetsController(window, view)
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    val pagerState = rememberPagerState(pageCount = {
        sharedViewModel.previewItems.value.size
    }, initialPage = sharedViewModel.previewIndex.value)


    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    PScaffold(
        navController,
        containerColor = Color.Black,
        navigationIcon = null,
        content = {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { page ->
                val image = sharedViewModel.previewItems.value[page]
                val imageSourceProvider = remember(key1 = image.uri) {
                    object : ImageSourceProvider {
                        override suspend fun provide(): Result<ComposeSubsamplingScaleImageSource> {
                            return Result.success(
                                ComposeSubsamplingScaleImageSource(
                                    inputStream = image.toInputStream(context)!!,
                                )
                            )
                        }
                    }
                }

                val eventListener = remember {
                    object : ComposeSubsamplingScaleImageEventListener() {
                        override fun onImageInfoDecoded(fullImageSize: IntSize) {
                            LogCat.d("DisplayFullImage: onImageInfoDecoded() fullImageSize=$fullImageSize")
                        }

                        override fun onFailedToDecodeImageInfo(error: Throwable) {
                            LogCat.d("DisplayFullImage: onFailedToDecodeImageInfo() error=${error.asLog()}")
                        }

                        override fun onTileDecoded(tileIndex: Int, totalTilesInTopLayer: Int) {
                            LogCat.d("DisplayFullImage: onTileDecoded() ${tileIndex}/${totalTilesInTopLayer}")
                        }

                        override fun onFailedToDecodeTile(
                            tileIndex: Int,
                            totalTilesInTopLayer: Int,
                            error: Throwable
                        ) {
                            LogCat.d("DisplayFullImage: onTileDecoded() ${tileIndex}/${totalTilesInTopLayer}, error=${error.asLog()}")
                        }

                        override fun onFullImageLoaded() {
                            LogCat.d("DisplayFullImage: onFullImageLoaded()")
                        }

                        override fun onFailedToLoadFullImage(error: Throwable) {
                            LogCat.e("DisplayFullImage: onFailedToLoadFullImage() error=${error.asLog()}")
                        }

                        override fun onInitializationCanceled() {
                            LogCat.e("DisplayFullImage: onInitializationCanceled()")
                        }
                    }
                }

                val state = rememberComposeSubsamplingScaleImageState(
                    maxScale = 3f,
                    doubleTapZoom = 2f,
                    scrollableContainerDirection = ScrollableContainerDirection.Horizontal,
                    debug = false
                )

                ComposeSubsamplingScaleImage(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    imageSourceProvider = imageSourceProvider,
                    eventListener = eventListener,
                    onImageTapped = { offset ->
                        LogCat.d("DisplayFullImage: Image tapped at $offset")
                    },
                    onImageLongTapped = { offset ->
                        LogCat.d("DisplayFullImage: Image long tapped at $offset")
                    },
                    fullImageLoadingContent = {
                        LogCat.d("fullImageLoadingContent: Image")
                        Text(text="Loading...")
                        CircularProgressIndicator(
                            modifier = Modifier.width(64.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            trackColor = MaterialTheme.colorScheme.secondary,
                        )
                    }
                )
            }
        }
    )
}
