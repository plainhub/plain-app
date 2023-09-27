package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.SrlClassicsFooterBinding
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState

class ClassicsFooter(context: Context, attrs: AttributeSet? = null) : ClassicsAbstract(context, attrs, 0), RefreshFooter {
    private val binding = SrlClassicsFooterBinding.inflate(LayoutInflater.from(context), this, true)

    private var mTextPulling = context.getString(R.string.srl_footer_pulling) // "上拉加载更多";
    private var mTextRelease = context.getString(R.string.srl_footer_release) // "释放立即加载";
    private var mTextLoading = context.getString(R.string.srl_footer_loading) // "正在加载...";
    private var mTextRefreshing = context.getString(R.string.srl_footer_refreshing) // "正在刷新...";
    private var mTextFinish = context.getString(R.string.srl_footer_finish) // "加载完成";
    private var mTextFailed = context.getString(R.string.srl_footer_failed) // "加载失败";
    private var mTextNothing = context.getString(R.string.srl_footer_nothing) // "没有更多数据了";
    private var mNoMoreData = false

    override fun onFinish(
        refreshLayout: RefreshLayout,
        success: Boolean,
    ): Int {
        super.onFinish(refreshLayout, success)
        if (!mNoMoreData) {
            binding.title.text = if (success) mTextFinish else mTextFailed
            return mFinishDuration
        }
        return 0
    }

    override fun setNoMoreData(noMoreData: Boolean): Boolean {
        if (mNoMoreData != noMoreData) {
            mNoMoreData = noMoreData
            binding.title.text =
                if (noMoreData) {
                    mTextNothing
                } else {
                    mTextPulling
                }
        }
        return true
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState,
    ) {
        if (!mNoMoreData) {
            when (newState) {
                RefreshState.None -> {
                    binding.title.text = mTextPulling
                }
                RefreshState.PullUpToLoad -> {
                    binding.title.text = mTextPulling
                }
                RefreshState.Loading, RefreshState.LoadReleased -> {
                    binding.title.text = mTextLoading
                }
                RefreshState.ReleaseToLoad -> {
                    binding.title.text = mTextRelease
                }
                RefreshState.Refreshing -> {
                    binding.title.text = mTextRefreshing
                }
                else -> {
                }
            }
        }
    }

    init {
        binding.title.text = mTextPulling
    }
}
