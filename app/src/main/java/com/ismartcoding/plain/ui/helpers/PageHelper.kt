package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.lib.statelayout.StateConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.views.ClassicsFooter
import com.ismartcoding.plain.ui.views.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

object PageHelper {
    fun init() {
        StateConfig.apply {
            emptyLayout = R.layout.view_rv_empty
            errorLayout = R.layout.view_rv_error
            loadingLayout = R.layout.view_rv_loading

            setRetryIds(R.id.msg)
        }

        SmartRefreshLayout.setDefaultRefreshInitializer { _, layout ->
            layout.setEnableLoadMore(false)
        }

        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            ClassicsHeader(context, layout)
        }

        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
            ClassicsFooter(context)
        }
    }
}
