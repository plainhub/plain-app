package com.ismartcoding.plain.ui.home.views

import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.HomeItemStorageBinding
import com.ismartcoding.plain.ui.audio.AudiosDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.file.FilesDialog
import com.ismartcoding.plain.ui.image.ImagesDialog
import com.ismartcoding.plain.ui.video.VideosDialog

fun HomeItemStorageBinding.initView() {
    title.setTextColor(title.context.getColor(R.color.primary))
    title.setText(R.string.home_item_title_storage)

    images.initTheme()
        .setKeyText(R.string.images)
        .showMore()
        .setClick {
            ImagesDialog().show()
        }

    audios.initTheme()
        .setKeyText(R.string.audios)
        .showMore()
        .setClick {
            AudiosDialog().show()
        }

    videos.initTheme()
        .setKeyText(R.string.videos)
        .showMore()
        .setClick {
            VideosDialog().show()
        }

    files.initTheme()
        .setKeyText(R.string.files)
        .showMore()
        .setClick {
            FilesDialog().show()
        }
}

