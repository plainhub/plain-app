package com.ismartcoding.plain.ui.preview.viewholders

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.databinding.ItemImageviewerSubsamplingBinding
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.utils.initTag
import kotlinx.coroutines.delay

class SubsamplingViewHolder(
    parent: ViewGroup,
    val binding: ItemImageviewerSubsamplingBinding =
        ItemImageviewerSubsamplingBinding.inflate(LayoutInflater.from(parent.context), parent, false),
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.subsamplingView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
    }

    fun bind(item: PreviewItem) {
        val holder = this
//        if (item.uri.endsWith(".gif", true)) {
//            binding.imageView.isVisible = true
//            binding.subsamplingView.isVisible = false
//            binding.imageView.initTag(item, holder)
//            binding.imageView.load(item.uri)
//        } else{
        binding.imageView.isVisible = false
        binding.subsamplingView.isVisible = true
        binding.subsamplingView.run {
            initTag(item, holder)
            orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
            coMain {
                val path = item.uri.toString()
                if (path.startsWith("http://", true) || path.startsWith("https://", true)) {
                    binding.loading.isVisible = true
                    Glide.with(context)
                        .asBitmap()
                        .load(path)
                        .into(
                            object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?,
                                ) {
                                    binding.loading.isVisible = false
                                    setImage(ImageSource.bitmap(resource))
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    binding.loading.isVisible = false
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                }
                            },
                        )
                } else if (path.startsWith("app://", true)) {
                    setImage(ImageSource.uri(path.getFinalPath(context)))
                } else {
                    binding.loading.isVisible = true
                    delay(100)
                    binding.loading.isVisible = false
                    setImage(ImageSource.uri(item.uri.toString()))
                }
            }
        }
        // }
    }
}
