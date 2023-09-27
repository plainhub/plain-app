package com.ismartcoding.lib.fragment

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentPagerAdapter(
    fa: Fragment,
    var fragments: List<Fragment>,
) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
