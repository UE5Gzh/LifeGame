package com.example.lifegame.ui.quest

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lifegame.databinding.FragmentQuestBinding
import com.example.lifegame.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuestFragment : BaseFragment<FragmentQuestBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentQuestBinding {
        return FragmentQuestBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        // Setup TabLayout and RecyclerView later
    }
}
