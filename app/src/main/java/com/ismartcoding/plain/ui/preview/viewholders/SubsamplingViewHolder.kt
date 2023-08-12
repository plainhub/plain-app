package com.ismartcoding.plain.ui.preview.viewholders

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.Coil.imageLoader
import coil.Coil.setImageLoader
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.databinding.ItemImageviewerSubsamplingBinding
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.utils.initTag
import kotlinx.coroutines.delay
import java.io.File

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
                        setImage(ImageSource.uri(item.uri.getFinalPath(context)))
                    } else {
                        binding.loading.isVisible = true
                        delay(100)
                        binding.loading.isVisible = false
                        setImage(ImageSource.uri(item.uri))
                    }
                }
            }
        //}
    }
}


