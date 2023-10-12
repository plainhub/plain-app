package com.ismartcoding.plain.ui.base.subsampling

import android.graphics.Bitmap

// TODO(KurobaEx): bitmap pooling?
internal class Tile(
  val x: Int,
  val y: Int
) {
  val xy: String
    get() = "${x}x${y}"

  @get:Synchronized
  @set:Synchronized
  var tileState: TileState? = null
    private set

  @get:Synchronized
  @set:Synchronized
  var sampleSize = 0
  @get:Synchronized
  @set:Synchronized
  var visible = false

  @get:Synchronized
  val isLoading: Boolean
    get() = tileState is TileState.Loading
  @get:Synchronized
  val isLoaded: Boolean
    get() = tileState is TileState.Loaded

  @get:Synchronized
  val canLoad: Boolean
    get() = tileState == null || tileState is TileState.Error

  // sRect
  @get:Synchronized
  val sourceRect: RectMut = RectMut(0, 0, 0, 0)

  // vRect
  @get:Synchronized
  val screenRect: RectMut = RectMut(0, 0, 0, 0)

  // fileSRect
  @get:Synchronized
  val fileSourceRect: RectMut = RectMut(0, 0, 0, 0)

  @Synchronized
  fun updateStateAsLoading(): Boolean {
    if (isLoading || isLoaded) {
      return false
    }

    tileState = TileState.Loading
    return true
  }

  @Synchronized
  fun onTileLoadError(error: Throwable) {
    tileState = TileState.Error(error)
  }

  @Synchronized
  fun onTileLoaded(decodedTileBitmap: Bitmap) {
    tileState = TileState.Loaded(decodedTileBitmap)
  }

  @Synchronized
  fun recycle() {
    visible = false

    (tileState as? TileState.Loaded)?.bitmap?.recycle()
    tileState = null
  }

  override fun toString(): String {
    return "Tile($xy, sampleSize=$sampleSize, tileState=$tileState, visible=$visible, " +
      "sourceRect=$sourceRect, screenRect=$screenRect, fileSourceRect=$fileSourceRect)"
  }

}

internal sealed class TileState {
  object Loading : TileState()
  class Loaded(val bitmap: Bitmap) : TileState()
  class Error(val error: Throwable) : TileState()

  override fun toString(): String {
    return when (this) {
      is Error -> "Error(message=${this.error.message})"
      is Loaded -> "Loaded(bitmap=${this.bitmap.width}x${this.bitmap.height}, recycled=${this.bitmap.isRecycled})"
      Loading -> "Loading"
    }
  }
}