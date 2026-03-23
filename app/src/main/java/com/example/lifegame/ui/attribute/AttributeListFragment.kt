package com.example.lifegame.ui.attribute

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.databinding.DialogAddAttributeBinding
import com.example.lifegame.databinding.DialogEditAttributeBinding
import com.example.lifegame.databinding.FragmentAttributeListBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class AttributeListFragment : BaseFragment<FragmentAttributeListBinding>() {

    private val viewModel: AttributeViewModel by activityViewModels()
    private lateinit var adapter: AttributeAdapter
    private var isSortMode = false
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeListBinding {
        return FragmentAttributeListBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        
        binding.root.setOnLongClickListener {
            toggleSortMode()
            true
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.attributes.collect { attributes ->
                        adapter.submitList(attributes)
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
            onAttributeLongClick = {
                if (!isSortMode) {
                    toggleSortMode()
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

    private fun toggleSortMode() {
        isSortMode = !isSortMode
        adapter.isSortMode = isSortMode
        if (isSortMode) {
            itemTouchHelper?.attachToRecyclerView(binding.rvAttributes)
            Toast.makeText(requireContext(), "进入排序模式，长按保存", Toast.LENGTH_SHORT).show()
        } else {
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

    private fun showAddAttributeDialog() {
        val dialogBinding = DialogAddAttributeBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

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

            val initialValue = initialValueStr?.toFloatOrNull() ?: 10f

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
        dialogBinding.etCurrentValue.setText(formatAttributeValue(attribute.currentValue))

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
            val currentValue = currentValueStr?.toFloatOrNull() ?: attribute.currentValue

            viewModel.updateAttribute(attribute.copy(
                currentValue = currentValue,
                colorHex = selectedColor
            ))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatAttributeValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    fun triggerAddAttribute() {
        showAddAttributeDialog()
    }

    fun triggerSortMode() {
        toggleSortMode()
    }

    fun isInSortMode(): Boolean = isSortMode
}
