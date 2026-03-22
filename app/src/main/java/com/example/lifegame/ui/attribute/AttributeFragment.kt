package com.example.lifegame.ui.attribute

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import com.example.lifegame.databinding.DialogAddAttributeBinding
import com.example.lifegame.databinding.DialogEditAttributeBinding
import com.example.lifegame.databinding.FragmentAttributeBinding
import com.example.lifegame.ui.attribute.rank.RankManagementFragment
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.ui.base.BaseFragment
import com.example.lifegame.ui.quest.QuestViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttributeFragment : BaseFragment<FragmentAttributeBinding>() {

    private val viewModel: AttributeViewModel by viewModels()
    private val questViewModel: QuestViewModel by viewModels()
    private lateinit var adapter: AttributeAdapter
    private var isSortMode = false
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeBinding {
        return FragmentAttributeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        setupRecyclerView()

        binding.btnAdd.setOnClickListener {
            showAddAttributeDialog()
        }

        binding.btnSort.setOnClickListener {
            toggleSortMode()
        }
    }

    private fun toggleSortMode() {
        isSortMode = !isSortMode
        adapter.isSortMode = isSortMode
        if (isSortMode) {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_save)
            binding.btnAdd.visibility = View.GONE
            itemTouchHelper?.attachToRecyclerView(binding.rvAttributes)
        } else {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
            itemTouchHelper?.attachToRecyclerView(null)
            saveSortOrder()
        }
    }

    private fun saveSortOrder() {
        val currentList = adapter.currentList
        val updatedAttributes = currentList.mapIndexed { index, attributeWithRanks ->
            attributeWithRanks.attribute.copy(sortOrder = index)
        }
        viewModel.updateAttributeSortOrders(updatedAttributes)
        Toast.makeText(requireContext(), "排序已保存", Toast.LENGTH_SHORT).show()
    }

    override fun observeData() {
        super.observeData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.attributesWithRanks.collect { attributes ->
                        adapter.submitList(attributes)
                    }
                }
                launch {
                    questViewModel.quests.collect { quests ->
                        val focusedQuest = quests.find { it.quest.isFocused }
                        if (focusedQuest != null && focusedQuest.quest.status == 0) {
                            binding.cardFocusedQuest.visibility = View.VISIBLE
                            binding.tvFocusedQuestName.text = focusedQuest.quest.name
                            
                            val progressFloat = questViewModel.calculateProgress(focusedQuest, viewModel.attributesWithRanks.value)
                            val progressInt = (progressFloat * 100).toInt()
                            binding.pbFocusedQuest.progress = progressInt
                            binding.tvFocusedQuestProgress.text = "${progressInt}%"
                        } else {
                            binding.cardFocusedQuest.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AttributeAdapter(
            onAttributeClick = { attributeWithRanks ->
                if (!isSortMode) {
                    showEditAttributeDialog(attributeWithRanks.attribute)
                }
            },
            onAttributeLongClick = { attributeWithRanks ->
                if (!isSortMode) {
                    showDeleteAttributeDialog(attributeWithRanks.attribute)
                }
            }
        )
        binding.rvAttributes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttributes.adapter = adapter

        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.swapItems(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            
            override fun isLongPressDragEnabled(): Boolean {
                return isSortMode
            }
        }
        itemTouchHelper = ItemTouchHelper(touchHelperCallback)
    }

    private fun showDeleteAttributeDialog(attribute: AttributeEntity) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("删除属性")
            .setMessage("确定要删除「${attribute.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.checkAndDeleteAttribute(attribute) { success, message ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddAttributeDialog() {
        val dialogBinding = DialogAddAttributeBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        // Setup color picker
        var selectedColor = ""
        val colorAdapter = ColorAdapter(null) { colorHex ->
            selectedColor = colorHex
        }
        dialogBinding.rvColors.layoutManager = GridLayoutManager(requireContext(), 6)
        dialogBinding.rvColors.adapter = colorAdapter

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim()
            val initialValueStr = dialogBinding.etInitialValue.text?.toString()?.trim()

            if (name.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "请输入属性名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val initialValue = initialValueStr?.toIntOrNull() ?: 10

            viewModel.addAttribute(name, initialValue, selectedColor)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditAttributeDialog(attribute: AttributeEntity) {
        val dialogBinding = DialogEditAttributeBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvDialogTitle.text = "修改 ${attribute.name}"
        dialogBinding.etCurrentValue.setText(attribute.currentValue.toString())

        var selectedColor = attribute.colorHex
        val colorAdapter = ColorAdapter(selectedColor) { colorHex ->
            selectedColor = colorHex
        }
        dialogBinding.rvColors.layoutManager = GridLayoutManager(requireContext(), 6)
        dialogBinding.rvColors.adapter = colorAdapter

        dialogBinding.btnManageRanks.setOnClickListener {
            dialog.dismiss()
            val args = Bundle().apply {
                putLong("attributeId", attribute.id)
            }
            findNavController().navigate(R.id.rankManagementFragment, args)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val currentValueStr = dialogBinding.etCurrentValue.text?.toString()?.trim()
            val currentValue = currentValueStr?.toIntOrNull() ?: attribute.currentValue

            viewModel.updateAttribute(attribute.copy(
                currentValue = currentValue,
                colorHex = selectedColor
            ))
            dialog.dismiss()
        }

        dialog.show()
    }

}

