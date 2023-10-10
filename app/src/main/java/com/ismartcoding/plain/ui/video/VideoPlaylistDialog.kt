package com.ismartcoding.plain.ui.video

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.annotaion.ItemOrientation
import com.ismartcoding.lib.brv.item.ItemDrag
import com.ismartcoding.lib.brv.listener.DefaultItemTouchCallback
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.VideoPlaylistPreference
import com.ismartcoding.plain.databinding.DialogPlaylistBinding
import com.ismartcoding.plain.databinding.ItemVideoBinding
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.video.DVideo
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.extensions.onSearch
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.TransitionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoPlaylistDialog : BaseBottomSheetDialog<DialogPlaylistBinding>() {
    data class SortableVideoModel(
        override val data: DVideo,
        override var itemOrientationDrag: Int = ItemOrientation.ALL,
    ) : VideoModel(data), ItemDrag

    private var searchQ: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.run {
            initMenu(R.menu.playlist, overflow = true)
            onMenuItemClick {
                when (itemId) {
                    R.id.clear_list -> {
                        val context = requireContext()
                        lifecycleScope.launch {
                            withIO { VideoPlaylistPreference.putAsync(context, arrayListOf()) }
                            dismiss()
                        }
                    }

                    R.id.cast -> {
                        val context = requireContext()
                        lifecycleScope.launch {
                            val items = withIO { VideoPlaylistPreference.getValueAsync(context) }
                            if (items.isEmpty()) {
                                DialogHelper.showMessage(R.string.add_videos_to_playlist)
                                return@launch
                            }
                            CastDialog(items).show()
                        }
                    }
                }
            }

            onSearch { q ->
                if (searchQ != q) {
                    searchQ = q
                    binding.list.page.refresh()
                }
            }
        }

        binding.list.rv.linear().setup {
            addType<SortableVideoModel>(R.layout.item_video)
            onBind {
                val m = getModel<SortableVideoModel>()
                val b = DataBindingUtil.bind<ItemVideoBinding>(itemView)!!
                TransitionHelper.put(m.data.id, b.image)
            }
            R.id.container.onClick {
                val m = getModel<SortableVideoModel>()
                val items = getModelList<SortableVideoModel>()
                PreviewDialog().show(
                    items = items.map { s -> PreviewItem(s.data.id, s.data.path.pathToUri(), s.data.path) },
                    initKey = m.data.id,
                )
            }

            itemTouchHelper =
                ItemTouchHelper(
                    object : DefaultItemTouchCallback() {
                        override fun onDrag(
                            source: BindingAdapter.BindingViewHolder,
                            target: BindingAdapter.BindingViewHolder,
                        ) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                VideoPlaylistPreference.putAsync(requireContext(), getModelList<SortableVideoModel>().map { it.data })
                            }
                        }
                    },
                )
        }

        binding.list.page.run {
            setEnableRefresh(false)
            setEnableNestedScroll(false)
            onRefresh {
                search()
            }
        }

        binding.list.page.stateLayout?.apply {
            canRetry = false
            onEmpty {
                findViewById<TextView>(R.id.msg).text = getString(R.string.video_playlist_no_data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.list.page.showLoading()
    }

    private fun search() {
        val context = requireContext()
        lifecycleScope.launch {
            binding.list.page.addData(
                withIO {
                    VideoPlaylistPreference.getValueAsync(context)
                        .filter { searchQ.isEmpty() || it.title.contains(searchQ, true) }
                        .map { v ->
                            SortableVideoModel(v).apply {
                                title = v.title
                                subtitle = FormatHelper.formatDuration(v.duration)
                                swipeEnable = true
                                rightSwipeText = getString(R.string.remove)
                                rightSwipeClick = {
                                    lifecycleScope.launch {
                                        withIO { VideoPlaylistPreference.deleteAsync(context, setOf(v.path)) }
                                        binding.list.rv.apply {
                                            val index = getModelList<SortableVideoModel>().indexOfFirst { it.data.path == v.path }
                                            if (index != -1) {
                                                removeModel(index)
                                            }
                                        }
                                        updateTitle()
                                    }
                                }
                                leftSwipeText = getString(R.string.cast)
                                leftSwipeClick = {
                                    CastDialog(arrayListOf(), v.path).show()
                                }
                            }
                        }
                },
            )
            updateTitle()
        }
    }

    private fun updateTitle() {
        lifecycleScope.launch {
            val total = withIO { VideoPlaylistPreference.getValueAsync(requireContext()).size }
            binding.topAppBar.title = if (total > 0) LocaleHelper.getStringF(R.string.playlist_title, "total", total) else getString(R.string.playlist)
        }
    }
}
