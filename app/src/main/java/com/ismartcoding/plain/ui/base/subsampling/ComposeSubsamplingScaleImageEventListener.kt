package com.ismartcoding.plain.ui.base.subsampling

import androidx.compose.ui.unit.IntSize

/**
 * Methods may get called on any thread (main/background)
 * */
open class ComposeSubsamplingScaleImageEventListener {
  /**
   * Some kind of an error occurred while during the [ImageSourceProvider.provide] method execution
   * */
  open fun onFailedToProvideSource(error: Throwable) { /**no-op*/ }

  /**
   * Called after the image dimensions were decoded
   * */
  open fun onImageInfoDecoded(fullImageSize: IntSize) { /**no-op*/ }

  /**
   * Called if we failed to decode image dimensions
   * */
  open fun onFailedToDecodeImageInfo(error: Throwable) { /**no-op*/ }

  /**
   * Called after a tile with index [tileIndex] is decoded
   * */
  open fun onTileDecoded(tileIndex: Int, totalTilesInTopLayer: Int) { /**no-op*/ }

  /**
   * Called if we failed to decode tile with index [tileIndex]
   * */
  open fun onFailedToDecodeTile(tileIndex: Int, totalTilesInTopLayer: Int, error: Throwable) { /**no-op*/ }

  /**
   * Called when all image base tiles (the top layer) were decoded or there were no tiles to decode
   * (empty image?)
   * */
  open fun onFullImageLoaded() { /**no-op*/ }

  /**
   * Called if we failed to decode the full image (might mean that the decoder was not
   * initialized for some weird reason)
   * */
  open fun onFailedToLoadFullImage(error: Throwable) { /**no-op*/ }

  open fun onInitializationCanceled() { /**no-op*/ }
}
