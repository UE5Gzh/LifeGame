package com.example.lifegame.ui.attribute.rank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifegame.databinding.DialogAddRankBinding
import com.example.lifegame.databinding.FragmentRankManagementBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RankManagementFragment : BaseFragment<FragmentRankManagementBinding>() {

    private val viewModel: RankViewModel by viewModels()
    private lateinit var adapter: RankAdapter
    private var attributeId: Long = -1

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRankManagementBinding {
        return FragmentRankManagementBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attributeId = arguments?.getLong("attributeId") ?: -1
        viewModel.setAttributeId(attributeId)
    }

    override fun setupViews() {
        super.setupViews()
        setupRecyclerView()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddRank.setOnClickListener {
            showAddRankDialog()
        }
    }

    override fun observeData() {
        super.observeData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ranks.collect { ranks ->
                    adapter.submitList(ranks)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = RankAdapter { rank ->
            MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle("删除段位")
                .setMessage("确定要删除段位「${rank.name}」吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteRank(rank)
                }
                .setNegativeButton("取消", null)
                .show()
        }
        binding.rvRanks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRanks.adapter = adapter
    }

    private fun showAddRankDialog() {
        val dialogBinding = DialogAddRankBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etRankName.text?.toString()?.trim()
            val minStr = dialogBinding.etMinValue.text?.toString()?.trim()
            val maxStr = dialogBinding.etMaxValue.text?.toString()?.trim()

            if (name.isNullOrEmpty() || minStr.isNullOrEmpty() || maxStr.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val minValue = minStr.toFloatOrNull()
            val maxValue = maxStr.toFloatOrNull()

            if (minValue == null || maxValue == null || minValue > maxValue) {
                return@setOnClickListener
            }

            val currentRanks = viewModel.ranks.value
            val hasOverlap = currentRanks.any { 
                (minValue >= it.minValue && minValue <= it.maxValue) ||
                (maxValue >= it.minValue && maxValue <= it.maxValue) ||
                (minValue <= it.minValue && maxValue >= it.maxValue)
            }

            if (hasOverlap) {
                return@setOnClickListener
            }

            viewModel.addRank(name, minValue, maxValue)
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        fun newInstance(attributeId: Long): RankManagementFragment {
            val fragment = RankManagementFragment()
            val args = Bundle()
            args.putLong("attributeId", attributeId)
            fragment.arguments = args
            return fragment
        }
    }
}
