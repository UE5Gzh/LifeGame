package com.example.lifegame.ui.attribute

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lifegame.databinding.FragmentStatusPlaceholderBinding
import com.example.lifegame.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatusPlaceholderFragment : BaseFragment<FragmentStatusPlaceholderBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStatusPlaceholderBinding {
        return FragmentStatusPlaceholderBinding.inflate(inflater, container, false)
    }
}
