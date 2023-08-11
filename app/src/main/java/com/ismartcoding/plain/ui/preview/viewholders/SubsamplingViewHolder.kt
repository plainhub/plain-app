package com.ismartcoding.plain.ui.preview.viewholders

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.databinding.ItemImageviewerSubsamplingBinding
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.utils.initTag
import kotlinx.coroutines.delay

class SubsamplingViewHolder(
    parent: ViewGroup,
    val binding: ItemImageviewerSubsamplingBinding =
        ItemImageviewerSubsamplingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.subsamplingView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
    }

    fun bind(item: PreviewItem) {
        val holder = this
        binding.subsamplingView.run {
            initTag(item, holder)
            orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
            coMain {
                if (item.uri.startsWith("http://", true) || item.uri.startsWith("https://", true)) {
                    val request = ImageRequest.Builder(context)
                        .data(item.uri)
                        .target(
                            onStart = { _ ->
                                binding.loading.isVisible = true
                            },
                            onSuccess = { result ->
                                binding.loading.isVisible = false
                                val bitmap = (result as? BitmapDrawable)?.bitmap
                                if (bitmap != null) {
                                    setImage(ImageSource.bitmap(bitmap))
                                }
                            },
                            onError = { _ ->
                                binding.loading.isVisible = false
                            }
                        )
                        .build()
                    context.imageLoader.enqueue(request)
                } else if (item.uri.startsWith("app://", true)) {
                    val path = context.getExternalFilesDir(null)?.path?.removeSuffix("/") + "/" + item.uri.substring("app://".length)
                    setImage(ImageSource.uri(path))
                } else {
                    binding.loading.isVisible = true
                    delay(100)
                    binding.loading.isVisible = false
                    setImage(ImageSource.uri(item.uri))
                }
            }
        }
    }
}


