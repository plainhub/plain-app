package com.ismartcoding.plain.ui.views.videoplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.LinearInterpolator
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.media.IVideoPlayer
import com.ismartcoding.lib.media.VideoPlayer
import com.ismartcoding.plain.databinding.ViewVideoPlayerFloatBinding
import kotlin.math.abs

class PlayerFloatWindow(context: Context) :
    AbstractMediaController(context.applicationContext) {
    private val binding =
        ViewVideoPlayerFloatBinding.inflate(
            LayoutInflater.from(context),
            this,
            true,
        )

    /** 窗体管理者：管理悬浮窗 */
    private lateinit var mWindowManager: WindowManager

    /** 悬浮窗布局参数 */
    private lateinit var mWindowLayoutParams: WindowManager.LayoutParams

    /** 动画时长 */
    private val DURATION_ANIMATION = 200L

    private var showAnimatorSet: AnimatorSet? = null
    private var hideAnimatorSet: AnimatorSet? = null

    private var touchSlop = 0
    private var screenWidth = 0
    private var screenHeight: Int = 0
    private var realScreenWidth: Int = 0

    /** 判断该悬浮窗是否可见 */
    var isShowing = false
        private set

    /** 是否执行动画隐藏中 */
    var isHiding = false
        private set

    override fun onBufferingUpdate(mp: IVideoPlayer) {
        super.onBufferingUpdate(mp)
        binding.playerLoadingView.visibility =
            if (mp.isBuffering() && !mp.isPlaying()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    init {
        initWindowManager(context.applicationContext)
        initView(context.applicationContext)
        initAnim()
    }

    private fun initWindowManager(context: Context) {
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mWindowLayoutParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.TOP or Gravity.START
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
    }

    private fun initView(context: Context) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        initRendView(binding.playerRenderView)
        initTouch()
        initScreenSize()
        initLocation()
    }

    private fun initTouch() {
        setOnTouchListener(
            object : OnTouchListener {
                var startRawX = 0f
                var startRawY = 0f
                var moveRawX = 0f
                var moveRawY = 0f
                var startX = 0f
                var startY = 0f
                var isMoving = false

                override fun onTouch(
                    v: View,
                    event: MotionEvent,
                ): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isMoving = false
                            startX = event.x
                            startY = event.y
                            startRawX = event.rawX
                            startRawY = event.rawY
                            return false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - startX
                            val dy = event.y - startY
                            if (!isMoving && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                                isMoving = true
                            }
                            if (isMoving) {
                                moveRawX = event.rawX
                                moveRawY = event.rawY
                                mWindowLayoutParams.let {
                                    it.x += (moveRawX - startRawX).toInt()
                                    it.y += (moveRawY - startRawY).toInt()
                                }
                                updateViewLayout()
                                startRawX = moveRawX
                                startRawY = moveRawY
                            }
                            return true
                        }
                        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                            // 解决onTouch和OnClick冲突
                            if (!isMoving) {
                                v.performClick()
                            }
                            return true
                        }
                    }
                    return true
                }
            },
        )
    }

    private fun initScreenSize() {
        val displayMetrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        val realDisplayMetrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getRealMetrics(realDisplayMetrics)
        realScreenWidth = realDisplayMetrics.widthPixels
    }

    private fun initLocation() {
        mWindowLayoutParams.let {
            val scale = context.resources.displayMetrics.density
            it.x = realScreenWidth - (scale * 234 + 0.5f).toInt()
            it.y = screenHeight - (scale * 186 + 0.5f).toInt()
        }
    }

    private fun updateViewLayout() {
        mWindowLayoutParams.let {
            val width = width
            val height = height
            it.x = if (it.x > realScreenWidth - width) realScreenWidth - width else it.x
            it.x = if (it.x < 0) 0 else it.x
            it.y = if (it.y > screenHeight - height) screenHeight - height else it.y
            it.y = if (it.y < 0) 0 else it.y
            try {
                // 部分手机在某些情况下会出现 非法参数异常
                mWindowManager.updateViewLayout(this, it) // 刷新显示
            } catch (e: IllegalArgumentException) {
                LogCat.e(e.toString())
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initScreenSize()
        updateViewLayout()
    }

    private fun initAnim() {
        showAnimatorSet = getShowAnimator()
        hideAnimatorSet = getHideAnimator()

        hideAnimatorSet?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (this@PlayerFloatWindow.isAttachedToWindow ||
                        this@PlayerFloatWindow.parent != null
                    ) {
                        try {
                            mWindowManager.removeViewImmediate(this@PlayerFloatWindow)
                        } catch (e: Exception) {
                        }
                    }
                    isHiding = false
                    isShowing = false
                }
            },
        )
    }

    private fun startShowAnim() {
        hideAnimatorSet?.cancel()
        showAnimatorSet?.let {
            it.cancel()
            it.start()
        }
    }

    private fun startHideAnim(): Boolean {
        showAnimatorSet?.cancel()
        hideAnimatorSet?.let {
            isHiding = true
            it.cancel()
            it.start()
            return true
        }
        return false
    }

    private fun getShowAnimator(): AnimatorSet {
        val alpha: ObjectAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f)
        val scaleX: ObjectAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f, 1f)
        val scaleY: ObjectAnimator = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = DURATION_ANIMATION
        animatorSet.interpolator = LinearInterpolator()
        animatorSet.playTogether(alpha, scaleX, scaleY)
        animatorSet.setTarget(this)
        return animatorSet
    }

    private fun getHideAnimator(): AnimatorSet {
        val alpha: ObjectAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f)
        val scaleX: ObjectAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 0f)
        val scaleY: ObjectAnimator = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 0f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = DURATION_ANIMATION
        animatorSet.interpolator = LinearInterpolator()
        animatorSet.playTogether(alpha, scaleX, scaleY)
        animatorSet.setTarget(this)
        return animatorSet
    }

    fun show(mediaPlayer: VideoPlayer) {
        if (!isShowing) {
            initScreenSize()
            try {
                mWindowManager.addView(this, mWindowLayoutParams)
                updateViewLayout()
                startShowAnim()
                isHiding = false
                isShowing = true
            } catch (e: Exception) {
                LogCat.e(e.toString())
                try {
                    mWindowManager.removeView(this)
                } catch (e: Exception) {
                }
                mediaPlayer.release() // 显示悬浮窗失败，则关闭播放器
                return
            }
        }

        bindMediaPlayer(mediaPlayer)
        play()
    }

    fun hide() {
        if (isShowing) {
            if (!startHideAnim()) {
                mWindowManager.removeView(this)
                isHiding = false
                isShowing = false
            }
        }
        unBindMediaPlayer()
    }

    fun close() {
        release()
        hide()
    }
}
