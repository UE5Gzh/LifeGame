package com.example.lifegame.ui.behavior

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lifegame.databinding.FragmentBehaviorBinding
import com.example.lifegame.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BehaviorFragment : BaseFragment<FragmentBehaviorBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBehaviorBinding {
        return FragmentBehaviorBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        // Setup RecyclerView later
    }
}
