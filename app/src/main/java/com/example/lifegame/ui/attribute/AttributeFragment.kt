package com.example.lifegame.ui.attribute

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.lifegame.R
import com.example.lifegame.databinding.FragmentAttributeBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttributeFragment : BaseFragment<FragmentAttributeBinding>() {

    private val viewModel: AttributeViewModel by viewModels()
    private lateinit var attributeListFragment: AttributeListFragment
    private lateinit var statusPlaceholderFragment: StatusPlaceholderFragment
    private lateinit var pagerAdapter: InfoPagerAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeBinding {
        return FragmentAttributeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        
        attributeListFragment = AttributeListFragment()
        statusPlaceholderFragment = StatusPlaceholderFragment()
        
        pagerAdapter = InfoPagerAdapter(requireActivity(), attributeListFragment, statusPlaceholderFragment)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_attribute)
                else -> getString(R.string.tab_status)
            }
        }.attach()

        binding.btnAdd.setOnClickListener {
            if (binding.viewPager.currentItem == 0) {
                attributeListFragment.triggerAddAttribute()
            }
        }

        binding.btnSort.setOnClickListener {
            if (binding.viewPager.currentItem == 0) {
                attributeListFragment.triggerSortMode()
                updateSortButton()
            }
        }
    }

    private fun updateSortButton() {
        if (attributeListFragment.isInSortMode()) {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_save)
            binding.btnAdd.visibility = View.GONE
        } else {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
        }
    }
}
