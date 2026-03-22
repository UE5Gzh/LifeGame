package com.example.lifegame.ui.log

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lifegame.databinding.FragmentLogBinding
import com.example.lifegame.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogFragment : BaseFragment<FragmentLogBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLogBinding {
        return FragmentLogBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        // Setup RecyclerView later
    }
}
