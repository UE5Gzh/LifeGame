package com.example.lifegame.ui.attribute

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class InfoPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val attributeListFragment: AttributeListFragment,
    private val statusPlaceholderFragment: StatusPlaceholderFragment
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> attributeListFragment
            else -> statusPlaceholderFragment
        }
    }
}
