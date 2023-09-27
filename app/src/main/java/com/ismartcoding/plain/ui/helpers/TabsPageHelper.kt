package com.ismartcoding.plain.ui.helpers

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ismartcoding.lib.fragment.FragmentPagerAdapter
import com.ismartcoding.plain.R

object TabsPageHelper {
    fun initTabsPager(
        fragment: DialogFragment,
        titleIds: List<Int>,
        fragments: List<Fragment>,
    ) {
        val pager = fragment.dialog?.findViewById<ViewPager2>(R.id.pager)
        pager?.apply {
            adapter = FragmentPagerAdapter(fragment, fragments)
            currentItem = 0
            offscreenPageLimit = fragments.size
        }

        fragment.dialog?.findViewById<TabLayout>(R.id.tabs)?.apply {
            TabLayoutMediator(this, pager!!) { tab, position ->
                tab.setText(titleIds[position])
            }.attach()
        }
    }
}
