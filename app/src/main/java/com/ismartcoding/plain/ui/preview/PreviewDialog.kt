package com.ismartcoding.plain.ui.preview

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.fullScreen
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogPreviewBinding
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.preview.utils.Config
import com.ismartcoding.plain.ui.preview.utils.Config.OFFSCREEN_PAGE_LIMIT
import com.ismartcoding.plain.ui.preview.utils.TransitionEndHelper
import com.ismartcoding.plain.ui.preview.utils.TransitionStartHelper
import com.ismartcoding.plain.ui.preview.utils.findViewWithKeyTag
import com.ismartcoding.plain.ui.preview.viewholders.VideoViewHolder

class PreviewDialog : DialogFragment() {
    private var _binding: DialogPreviewBinding? = null
    val binding get() = _binding!!
    private val viewModel: PreviewViewModel by viewModels()
    private val adapter by lazy { PreviewAdapter(initKey) }
    private var lastVideoVH: RecyclerView.ViewHolder? = null
    private lateinit var list: List<PreviewItem>
    private var initKey: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.Theme_FullScreen).apply {
            setCanceledOnTouchOutside(true)
            window?.let { win ->
                win.fullScreen()
                win.setWindowAnimations(R.style.Animation_Keep)
                win.setGravity(Gravity.CENTER)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        view?.requestFocus()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initEvents()

        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, keyCode, event ->
            val backPressed =
                event.action == MotionEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK
            if (backPressed) {
                onBackPressed()
            }
            backPressed
        }

        binding.viewer.run {
            (getChildAt(0) as? RecyclerView?)?.let {
                it.clipChildren = false
                it.itemAnimator = null
            }
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            registerOnPageChangeCallback(pagerCallback)
            offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
        }
        binding.viewer.adapter = adapter

        val pagingData = PagingData.from(list)
        adapter.submitData(lifecycle, pagingData)
        binding.viewer.setCurrentItem(list.indexOfFirst { it.id == initKey }, false)

        viewModel.viewerUserInputEnabled.observe(viewLifecycleOwner) {
            binding.viewer.isUserInputEnabled = it ?: true
        }
    }

    private fun initEvents() {
        receiveEvent<ViewerDismissEvent> {
            onBackPressed()
        }

        receiveEvent<ViewerShowCastListEvent> { event ->
            CastDialog(arrayListOf(), event.uri).show()
        }

        receiveEvent<ViewerInitEvent> { event ->
            TransitionStartHelper.start(
                this@PreviewDialog,
                TransitionHelper.provide(initKey),
                event.viewHolder,
            )
            binding.background.changeToBackgroundColor(Config.VIEWER_BACKGROUND_COLOR)
            playVideo(event.viewHolder)
        }

        receiveEvent<ViewerDragEvent> { event ->
            binding.background.updateBackgroundColor(
                event.fraction,
                Config.VIEWER_BACKGROUND_COLOR,
                Color.TRANSPARENT,
            )
        }

        receiveEvent<ViewerRestoreEvent> { _ ->
            binding.background.changeToBackgroundColor(Config.VIEWER_BACKGROUND_COLOR)
        }

        receiveEvent<ViewerReleaseEvent> { event ->
            val startView =
                (event.view.getTag(R.id.viewer_adapter_item_key))?.let { TransitionHelper.provide(it as String) }
            setStatusBarTransparent()
            binding.background.changeToBackgroundColor(Color.TRANSPARENT)
            TransitionEndHelper.end(
                this@PreviewDialog,
                startView,
                event.view.getTag(R.id.viewer_adapter_item_holder) as RecyclerView.ViewHolder,
            )
        }
    }

    private fun setStatusBarTransparent() {
        dialog?.window?.run {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
        }
    }

    private val pagerCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                (binding.viewer.getChildAt(0) as RecyclerView).findViewHolderForAdapterPosition(
                    position,
                )?.let {
                    playVideo(it)
                }
            }
        }
    }

    private fun playVideo(holder: RecyclerView.ViewHolder) {
        (lastVideoVH as? VideoViewHolder)?.pause()
        lastVideoVH = null
        if (holder is VideoViewHolder) {
            lastVideoVH = holder
            holder.resume()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.setOnKeyListener(null)
        (lastVideoVH as? VideoViewHolder)?.release()
        binding.viewer.unregisterOnPageChangeCallback(pagerCallback)
        _binding = null
    }

    private fun onBackPressed() {
        if (TransitionStartHelper.transitionAnimating || TransitionEndHelper.transitionAnimating) return

        list.getOrNull(binding.viewer.currentItem)?.id?.let { currentKey ->
            binding.viewer.findViewWithKeyTag(R.id.viewer_adapter_item_key, currentKey)
                ?.let { endView ->
                    setStatusBarTransparent()
                    binding.background.changeToBackgroundColor(Color.TRANSPARENT)
                    (endView.getTag(R.id.viewer_adapter_item_holder) as? RecyclerView.ViewHolder?)?.let {
                        TransitionEndHelper.end(this, TransitionHelper.provide(currentKey), it)
                    }
                }
        }
    }

    fun show(
        items: List<PreviewItem>,
        initKey: String = "",
    ) {
        list = items
        this.initKey = initKey
        super.show(
            MainActivity.instance.get()!!.supportFragmentManager,
            this.javaClass.simpleName,
        )
    }
}
