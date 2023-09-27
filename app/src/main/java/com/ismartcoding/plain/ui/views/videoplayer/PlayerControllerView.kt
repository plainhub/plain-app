package com.ismartcoding.plain.ui.views.videoplayer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.media.IVideoPlayer
import com.ismartcoding.lib.media.VideoPlayer
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ViewVideoPlayerBinding

open class PlayerControllerView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : AbstractMediaController(context, attrs, defStyleAttr) {
        val binding =
            ViewVideoPlayerBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )

        private val animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        private val animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)

        private fun hideFloatView() {
            binding.bottomLayout.startAnimation(animOut)
            binding.bottomLayout.visibility = View.GONE
        }

        private fun showFloatView() {
            binding.bottomLayout.startAnimation(animIn)
            binding.bottomLayout.visibility = View.VISIBLE
        }

        private val mPlayerGesture: PlayerGesture by lazy {
            PlayerGesture(context).apply {
                onSingleTapUp = {
                    if (binding.bottomLayout.visibility == View.VISIBLE) {
                        hideFloatView()
                    } else {
                        showFloatView()
                    }
                    true
                }

                onFingerUp = {
                    if (toGestureSeek) {
                        seekTo(gestureSeekPosition)
                        toGestureSeek = false
                    }
                }

                onScrollLeftRight = { width, distance ->
                    mMediaPlayer?.let {
                        if (it.isInPlaybackState()) {
                            if (!toGestureSeek) {
                                gestureSeekPosition = it.getCurrentPosition()
                                toGestureSeek = true
                            }
                            val percent = it.getDuration() / width.toFloat()
                            val seekChange = (distance * percent).toLong()
                            gestureSeekPosition -= seekChange
                            if (gestureSeekPosition > it.getDuration()) {
                                gestureSeekPosition = it.getDuration()
                            } else if (gestureSeekPosition < 0) {
                                gestureSeekPosition = 0
                            }

                            binding.currentTime.text = FormatHelper.formatDuration(gestureSeekPosition / 1000)
                            setSeekbarProgress(gestureSeekPosition, it.getDuration())
                        }
                    }
                }
            }
        }

        /** 是否在拖动进度条 */
        private var isDragging = false

        /** 默认开启手势 */
        private var enableGesture = true

        /** 默认开启seek手势 */
        private var enableSeekGesture = true

        /** 是否需要执行手势seek */
        private var toGestureSeek = false

        /** 执行手势seek位置 */
        private var gestureSeekPosition: Long = 0

        init {
            initView()
        }

        private fun initView() {
            initRendView(binding.renderView)

            initPlayPause()

            initSeekBar()
        }

        private fun initPlayPause() {
            binding.playPause.let { playPause ->
                playPause.tag = false
                playPause.setOnClickListener {
                    if (it.tag as Boolean) {
                        pause()
                        binding.centerPlayPause.visibility = View.VISIBLE
                    } else {
                        play()
                    }
                }
            }
            binding.centerPlayPause.setOnClickListener {
                play()
                it.visibility = View.GONE
            }
        }

        private fun initSeekBar() {
            binding.seekBar.let { playerSeekBar ->
                playerSeekBar.max = 1000
                playerSeekBar.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        var newPosition: Long = 0

                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean,
                        ) {
                            if (!fromUser) { // 不是用户主动拉动进度条，则不需要seek
                                return
                            }
                            mMediaPlayer?.let { mp ->
                                newPosition = mp.getDuration() * progress / seekBar.max
                                binding.currentTime.text = FormatHelper.formatDuration(newPosition / 1000)
                                binding.centerPlayPause.run {
                                    if (mp.isInPlaybackState()) {
                                        visibility = View.GONE
                                        binding.playPause.setImageResource(R.drawable.ic_pause)
                                    }
                                }
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            isDragging = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            isDragging = false
                            mMediaPlayer?.seekTo(newPosition)
                        }
                    },
                )
            }
        }

        private fun setSeekbarProgress(
            currentPosition: Long,
            duration: Long,
        ) {
            binding.seekBar.let { playerSeekBar ->
                val progress =
                    if (duration > 0) {
                        val pos: Long = playerSeekBar.max * currentPosition / duration
                        pos.toInt()
                    } else {
                        0
                    }
                playerSeekBar.progress = progress
            }
        }

        override fun onCompletion(mp: IVideoPlayer) {
            super.onCompletion(mp)
            if (!isDragging) {
                binding.seekBar.apply {
                    progress = max
                }
            }
        }

        override fun autoUpdateInfo(mp: VideoPlayer) {
            if (!toGestureSeek && !isDragging) { // 非手势操作时，才执行自动更新进度
                super.autoUpdateInfo(mp)
                setSeekbarProgress(mp.getCurrentPosition(), mp.getDuration())
            }
        }

        override fun onBufferingUpdate(mp: IVideoPlayer) {
            super.onBufferingUpdate(mp)
            binding.loading.let {
                if (mp.isBuffering() && !mp.isPlaying()) {
                    it.visibility = View.VISIBLE
                } else {
                    it.visibility = View.GONE
                }
            }
        }

        override fun updatePlayerState(isPlaying: Boolean) {
            super.updatePlayerState(isPlaying)
            binding.playPause.let {
                it.tag = isPlaying
                if (isPlaying) {
                    it.setImageResource(R.drawable.ic_pause)
                } else {
                    it.setImageResource(R.drawable.ic_play)
                }
            }
            binding.centerPlayPause.visibility =
                if (isPlaying) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        override fun updateCurrentPosition(position: Long) {
            super.updateCurrentPosition(position)
            binding.currentTime.text = FormatHelper.formatDuration(position / 1000)
        }

        override fun updateDuration(duration: Long) {
            super.updateDuration(duration)
            binding.totalTime.text = FormatHelper.formatDuration(duration / 1000)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return if (enableGesture) {
                mPlayerGesture.onTouchEvent(event, width)
            } else {
                super.onTouchEvent(event)
            }
        }
    }
