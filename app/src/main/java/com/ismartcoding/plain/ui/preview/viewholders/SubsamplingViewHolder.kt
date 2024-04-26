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
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.plain.databinding.ItemImageviewerSubsamplingBinding
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.utils.initTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SubsamplingViewHolder(
    parent: ViewGroup,
    val binding: ItemImageviewerSubsamplingBinding =
        ItemImageviewerSubsamplingBinding.inflate(LayoutInflater.from(parent.context), parent, false),
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.subsamplingView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
    }

    private var showLoadingJob: Job? = null
    fun bind(item: PreviewItem) {
        val holder = this
        binding.download.setSafeClick {
            coMain {
                val r = withIO { if (item.path.isNotEmpty()) FileHelper.copyFileToDownloads(item.path) else FileHelper.copyFileToDownloads(binding.download.context, item.uri) }
                if (r.isNotEmpty()) {
                    DialogHelper.showMessage("Downloaded to $r")
                } else {
                    DialogHelper.showMessage("Failed to download.")
                }
            }
        }
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
                    showLoadingJob?.cancel()
                    showLoadingJob = coIO {
                        delay(200)
                        coMain {
                            binding.loading.isVisible = true
                        }
                    }
                    Glide.with(context)
                        .asBitmap()
                        .load(path)
                        .into(
                            object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?,
                                ) {
                                    showLoadingJob?.cancel()
                                    binding.loading.isVisible = false
                                    setImage(ImageSource.bitmap(resource))
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    showLoadingJob?.cancel()
                                    binding.loading.isVisible = false
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                }
                            },
                        )
                } else if (path.startsWith("app://", true)) {
                    setImage(ImageSource.uri(path.getFinalPath(context)))
                } else {
                    if (isSPlus()) {
                        setImage(ImageSource.uri(item.uri.toString()))
                    } else { // could be slow on poor performance devices
                        coIO {
                            delay(200)
                            coMain {
                                setImage(ImageSource.uri(item.uri.toString()))
                            }
                        }
                    }
                }
            }
        }
        // }
    }
}
