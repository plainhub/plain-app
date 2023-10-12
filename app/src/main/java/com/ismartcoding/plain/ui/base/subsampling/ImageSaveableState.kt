package com.ismartcoding.plain.ui.base.subsampling

import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable

data class ImageSaveableState(
  val scale: Float,
  val center: PointF
) : Parcelable {

  constructor(parcel: Parcel) : this(
    parcel.readFloat(), // scale
    PointF(
      parcel.readFloat(), // centerX
      parcel.readFloat()  // centerY
    )
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeFloat(scale)
    parcel.writeFloat(center.x)
    parcel.writeFloat(center.y)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<ImageSaveableState> {
    override fun createFromParcel(parcel: Parcel): ImageSaveableState {
      return ImageSaveableState(parcel)
    }

    override fun newArray(size: Int): Array<ImageSaveableState?> {
      return arrayOfNulls(size)
    }
  }
}
