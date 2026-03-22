package com.example.lifegame.ui.behavior

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.databinding.DialogAddBehaviorBinding
import com.example.lifegame.databinding.FragmentBehaviorBinding
import com.example.lifegame.databinding.ItemBehaviorAttributeModifierBinding
import com.example.lifegame.ui.base.BaseFragment
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BehaviorFragment : BaseFragment<FragmentBehaviorBinding>() {

    private val viewModel: BehaviorViewModel by viewModels()
    private lateinit var adapter: BehaviorAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBehaviorBinding {
        return FragmentBehaviorBinding.inflate(inflater, container, false)
    }

    private fun showDeleteBehaviorDialog(behaviorWithModifiers: BehaviorWithModifiers) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("删除行为")
            .setMessage("确定要删除「${behaviorWithModifiers.behavior.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteBehavior(behaviorWithModifiers.behavior)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditBehaviorDialog(behaviorWithModifiers: BehaviorWithModifiers) {
        val dialogBinding = DialogAddBehaviorBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialogBinding.tvDialogTitle.text = "修改行为"

        val behavior = behaviorWithModifiers.behavior
        dialogBinding.etName.setText(behavior.name)
        if (behavior.energyType == 0) {
            dialogBinding.rbConsume.isChecked = true
        } else {
            dialogBinding.rbRestore.isChecked = true
        }
        dialogBinding.etEnergyValue.setText(behavior.energyValue.toString())
        dialogBinding.etFocusDuration.setText(behavior.focusDuration.toString())

        val availableAttributes = viewModel.attributes.value

        fun addAttributeRow(preSelectedAttributeId: Long? = null, preValueChange: Int = 1) {
            if (availableAttributes.isEmpty()) {
                Toast.makeText(requireContext(), "请先在个人属性中添加属性", Toast.LENGTH_SHORT).show()
                return
            }

            val rowBinding = ItemBehaviorAttributeModifierBinding.inflate(
                layoutInflater,
                dialogBinding.llAttributesContainer,
                true
            )

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                availableAttributes.map { it.attribute.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerAttribute.adapter = adapter

            if (preSelectedAttributeId != null) {
                val index = availableAttributes.indexOfFirst { it.attribute.id == preSelectedAttributeId }
                if (index >= 0) {
                    rowBinding.spinnerAttribute.setSelection(index)
                }
            }
            
            rowBinding.etValueChange.setText(preValueChange.toString())

            rowBinding.btnRemove.setOnClickListener {
                dialogBinding.llAttributesContainer.removeView(rowBinding.root)
            }
        }

        // Add rows for existing modifiers
        for (modifier in behaviorWithModifiers.modifiers) {
            addAttributeRow(modifier.attributeId, modifier.valueChange)
        }

        dialogBinding.btnAddAttributeModifier.setOnClickListener {
            addAttributeRow()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim()
            val energyType = if (dialogBinding.rbConsume.isChecked) 0 else 1
            val energyValueStr = dialogBinding.etEnergyValue.text?.toString()?.trim()
            val focusDurationStr = dialogBinding.etFocusDuration.text?.toString()?.trim()

            if (name.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "请输入行为名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val energyValue = energyValueStr?.toIntOrNull() ?: 10
            val focusDuration = focusDurationStr?.toIntOrNull() ?: 0

            val modifiers = mutableListOf<Pair<Long, Int>>()
            for (i in 0 until dialogBinding.llAttributesContainer.childCount) {
                val child = dialogBinding.llAttributesContainer.getChildAt(i)
                val rowBinding = ItemBehaviorAttributeModifierBinding.bind(child)
                
                val selectedIndex = rowBinding.spinnerAttribute.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < availableAttributes.size) {
                    val attributeId = availableAttributes[selectedIndex].attribute.id
                    val valueChangeStr = rowBinding.etValueChange.text?.toString()?.trim()
                    val valueChange = valueChangeStr?.toIntOrNull() ?: 0
                    if (valueChange != 0) {
                        modifiers.add(Pair(attributeId, valueChange))
                    }
                }
            }

            viewModel.updateBehavior(behavior.id, name, energyType, energyValue, focusDuration, modifiers)
            dialog.dismiss()
        }

        dialog.show()
    }
    override fun setupViews() {
        super.setupViews()
        
        setupRecyclerView()

        binding.btnAdd.setOnClickListener {
            showAddBehaviorDialog()
        }

        binding.btnResetEnergy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle("重置精力")
                .setMessage("确定要将精力恢复到最大值吗？")
                .setPositiveButton("确定") { _, _ ->
                    viewModel.resetEnergy()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        binding.cardEnergy.setOnLongClickListener {
            showSetMaxEnergyDialog()
            true
        }
    }

    private fun showSetMaxEnergyDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(viewModel.maxEnergy.value.toString())
            setSelection(text.length)
        }

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("设置最大精力值")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newMax = editText.text.toString().toIntOrNull()
                if (newMax != null && newMax > 0) {
                    viewModel.setMaxEnergy(newMax)
                } else {
                    Toast.makeText(requireContext(), "请输入有效的正整数", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = BehaviorAdapter(
            onActionClick = { behavior ->
                viewModel.executeBehavior(behavior)
                Toast.makeText(requireContext(), "执行了: ${behavior.behavior.name}", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { behavior ->
                showEditBehaviorDialog(behavior)
            },
            onItemLongClick = { behavior ->
                showDeleteBehaviorDialog(behavior)
            }
        )
        binding.rvBehaviors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBehaviors.adapter = adapter
    }

    override fun observeData() {
        super.observeData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.attributes.collect { attributes ->
                        adapter.setAttributes(attributes)
                    }
                }
                launch {
                    viewModel.behaviors.collect { behaviors ->
                        adapter.submitList(behaviors)
                    }
                }
                launch {
                    viewModel.currentEnergy.collect { energy ->
                        binding.pbEnergy.progress = energy
                        updateEnergyText()
                    }
                }
                launch {
                    viewModel.maxEnergy.collect { maxEnergy ->
                        binding.pbEnergy.max = maxEnergy
                        updateEnergyText()
                    }
                }
            }
        }
    }

    private fun updateEnergyText() {
        val current = viewModel.currentEnergy.value
        val max = viewModel.maxEnergy.value
        binding.tvEnergyText.text = "$current / $max"
    }

    private fun showAddBehaviorDialog() {
        val dialogBinding = DialogAddBehaviorBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)

        val availableAttributes = viewModel.attributes.value

        // Helper to add an attribute modifier row
        fun addAttributeRow() {
            if (availableAttributes.isEmpty()) {
                Toast.makeText(requireContext(), "请先在个人属性中添加属性", Toast.LENGTH_SHORT).show()
                return
            }

            val rowBinding = ItemBehaviorAttributeModifierBinding.inflate(
                layoutInflater,
                dialogBinding.llAttributesContainer,
                true
            )

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                availableAttributes.map { it.attribute.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerAttribute.adapter = adapter

            rowBinding.btnRemove.setOnClickListener {
                dialogBinding.llAttributesContainer.removeView(rowBinding.root)
            }
        }

        // Add first row by default if attributes exist
        if (availableAttributes.isNotEmpty()) {
            addAttributeRow()
        }

        dialogBinding.btnAddAttributeModifier.setOnClickListener {
            addAttributeRow()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim()
            val energyType = if (dialogBinding.rbConsume.isChecked) 0 else 1
            val energyValueStr = dialogBinding.etEnergyValue.text?.toString()?.trim()
            val focusDurationStr = dialogBinding.etFocusDuration.text?.toString()?.trim()

            if (name.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "请输入行为名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val energyValue = energyValueStr?.toIntOrNull() ?: 10
            val focusDuration = focusDurationStr?.toIntOrNull() ?: 0

            val modifiers = mutableListOf<Pair<Long, Int>>()
            for (i in 0 until dialogBinding.llAttributesContainer.childCount) {
                val child = dialogBinding.llAttributesContainer.getChildAt(i)
                val rowBinding = ItemBehaviorAttributeModifierBinding.bind(child)
                
                val selectedIndex = rowBinding.spinnerAttribute.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < availableAttributes.size) {
                    val attributeId = availableAttributes[selectedIndex].attribute.id
                    val valueChangeStr = rowBinding.etValueChange.text?.toString()?.trim()
                    val valueChange = valueChangeStr?.toIntOrNull() ?: 0
                    if (valueChange != 0) {
                        modifiers.add(Pair(attributeId, valueChange))
                    }
                }
            }

            viewModel.addBehavior(name, energyType, energyValue, focusDuration, modifiers)
            dialog.dismiss()
        }

        dialog.show()
    }
}
