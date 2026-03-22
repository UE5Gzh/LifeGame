package com.example.lifegame.ui.attribute

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lifegame.databinding.FragmentAttributeBinding
import com.example.lifegame.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttributeFragment : BaseFragment<FragmentAttributeBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeBinding {
        return FragmentAttributeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        // Setup RecyclerView later
    }
}
