package com.example.lifegame.ui.attribute

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.lifegame.R
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.databinding.DialogAddStatusMultiEffectBinding
import com.example.lifegame.databinding.FragmentAttributeBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttributeFragment : BaseFragment<FragmentAttributeBinding>() {

    private val viewModel: AttributeViewModel by viewModels()
    private val statusViewModel: StatusViewModel by viewModels()

    private val colorOptions = listOf(
        "#21212B", "#1A237E", "#0D47A1", "#01579B",
        "#006064", "#004D40", "#1B5E20", "#33691E",
        "#827717", "#F57F17", "#FF6F00", "#E65100",
        "#BF360C", "#3E2723", "#4A148C", "#880E4F"
    )
    private var selectedColorIndex = 0
    private var editingStatus: StatusEntity? = null
    private var editingEffects: List<StatusEffectEntity>? = null

    private var attributeListFragment: AttributeListFragment? = null
    private var statusPlaceholderFragment: StatusPlaceholderFragment? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeBinding {
        return FragmentAttributeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        
        val pagerAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                return when (position) {
                    0 -> AttributeListFragment().also { attributeListFragment = it }
                    else -> StatusPlaceholderFragment().also { statusPlaceholderFragment = it }
                }
            }
        }
        
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_attribute)
                else -> getString(R.string.tab_status)
            }
        }.attach()

        binding.btnAdd.setOnClickListener {
            showAddMenu()
        }

        binding.btnSort.setOnClickListener {
            toggleSortModeForCurrentPage()
        }
    }

    private fun toggleSortModeForCurrentPage() {
        val currentPosition = binding.viewPager.currentItem
        
        if (currentPosition == 0) {
            attributeListFragment?.let { fragment ->
                fragment.triggerSortMode()
                updateSortButton(fragment.isInSortMode())
            }
        } else {
            statusPlaceholderFragment?.let { fragment ->
                fragment.triggerSortMode()
                updateSortButton(fragment.isInSortMode())
            }
        }
    }

    private fun showAddMenu() {
        val items = arrayOf("新增属性", "新增状态")
        
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.WHITE)
                textView.textSize = 16f
                textView.setPadding(32, 24, 32, 24)
                return view
            }
        }
        
        AlertDialog.Builder(requireContext(), R.style.Theme_LifeGame_Dialog)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> attributeListFragment?.triggerAddAttribute()
                    1 -> showAddStatusDialog(null)
                }
            }
            .show()
    }

    private fun updateSortButton(isSortMode: Boolean) {
        if (isSortMode) {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_save)
            binding.btnAdd.visibility = View.GONE
        } else {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
        }
    }

    fun showAddStatusDialog(status: StatusEntity?) {
        editingStatus = status
        val dialogBinding = DialogAddStatusMultiEffectBinding.inflate(LayoutInflater.from(requireContext()))
        
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_LifeGame_Dialog)
            .setView(dialogBinding.root)
            .create()
        
        var effectsAdapter: StatusEffectAdapter? = null
        effectsAdapter = StatusEffectAdapter(emptyList()) { position ->
            effectsAdapter?.removeEffect(position)
        }
        dialogBinding.rvEffects.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvEffects.adapter = effectsAdapter
        
        setupColorPicker(dialogBinding)
        setupDurationControls(dialogBinding)
        
        viewLifecycleOwner.lifecycleScope.launch {
            statusViewModel.attributes.collect { attributes ->
                if (attributes.isNotEmpty()) {
                    effectsAdapter.updateAttributes(attributes)
                    
                    if (status != null && editingEffects == null) {
                        val existingEffects = statusViewModel.getEffectsForStatus(status.id)
                        editingEffects = existingEffects
                        val effectItems = existingEffects.map { EffectItem.fromStatusEffectEntity(it) }
                        effectsAdapter.setEffects(effectItems)
                    }
                }
            }
        }
        
        if (status != null) {
            dialogBinding.tvDialogTitle.text = "编辑状态"
            dialogBinding.etName.setText(status.name)
            dialogBinding.etDescription.setText(status.description)
            selectedColorIndex = colorOptions.indexOf(status.colorHex).takeIf { it >= 0 } ?: 0
            updateColorSelection(dialogBinding)
            
            if (status.durationValue > 0) {
                dialogBinding.cbHasDuration.isChecked = true
                dialogBinding.llDurationParams.visibility = View.VISIBLE
                dialogBinding.etDurationValue.setText(status.durationValue.toString())
                dialogBinding.spinnerDurationUnit.setSelection(status.durationUnit)
            }
        }
        
        dialogBinding.btnAddPeriodic.setOnClickListener {
            val attributes = statusViewModel.attributes.value
            val targetAttrId = attributes.firstOrNull()?.attribute?.id ?: 0L
            effectsAdapter.addEffect(EffectItem(
                effectType = 0,
                targetAttributeId = targetAttrId,
                periodValue = 1,
                periodUnit = 0,
                changeValue = 0f
            ))
        }
        
        dialogBinding.btnAddBonus.setOnClickListener {
            val attributes = statusViewModel.attributes.value
            val targetAttrId = attributes.firstOrNull()?.attribute?.id ?: 0L
            effectsAdapter.addEffect(EffectItem(
                effectType = 1,
                targetAttributeId = targetAttrId,
                bonusPercent = 10f
            ))
        }
        
        dialogBinding.btnAddDecay.setOnClickListener {
            val attributes = statusViewModel.attributes.value
            val targetAttrId = attributes.firstOrNull()?.attribute?.id ?: 0L
            effectsAdapter.addEffect(EffectItem(
                effectType = 2,
                targetAttributeId = targetAttrId,
                bonusPercent = 10f
            ))
        }
        
        dialogBinding.btnCancel.setOnClickListener { 
            editingStatus = null
            editingEffects = null
            dialog.dismiss() 
        }
        
        dialogBinding.btnConfirm.setOnClickListener {
            if (validateInput(dialogBinding, effectsAdapter)) {
                saveStatus(dialogBinding, effectsAdapter)
                dialog.dismiss()
            }
        }
        
        dialog.setOnDismissListener {
            editingStatus = null
            editingEffects = null
        }
        
        dialog.show()
    }

    private fun setupColorPicker(dialogBinding: DialogAddStatusMultiEffectBinding) {
        dialogBinding.rvColors.layoutManager = GridLayoutManager(requireContext(), 4)
        dialogBinding.rvColors.adapter = ColorPickerAdapter(colorOptions) { position ->
            selectedColorIndex = position
            updateColorSelection(dialogBinding)
        }
        updateColorSelection(dialogBinding)
    }

    private fun updateColorSelection(dialogBinding: DialogAddStatusMultiEffectBinding) {
        val adapter = dialogBinding.rvColors.adapter as? ColorPickerAdapter
        adapter?.selectedPosition = selectedColorIndex
        adapter?.notifyDataSetChanged()
    }

    private fun setupDurationControls(dialogBinding: DialogAddStatusMultiEffectBinding) {
        val units = arrayOf("分钟", "小时", "天")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_dark, units)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        dialogBinding.spinnerDurationUnit.adapter = adapter
        
        dialogBinding.cbHasDuration.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.llDurationParams.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun validateInput(
        dialogBinding: DialogAddStatusMultiEffectBinding,
        effectsAdapter: StatusEffectAdapter
    ): Boolean {
        val name = dialogBinding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请输入状态名称", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val effects = effectsAdapter.getEffects()
        if (effects.isEmpty()) {
            Toast.makeText(requireContext(), "请至少添加一个效果", Toast.LENGTH_SHORT).show()
            return false
        }
        
        for ((index, effect) in effects.withIndex()) {
            val attributes = statusViewModel.attributes.value
            val attrName = attributes.find { it.attribute.id == effect.targetAttributeId }?.attribute?.name ?: "未知属性"
            
            when (effect.effectType) {
                0 -> {
                    if (effect.periodValue <= 0) {
                        Toast.makeText(requireContext(), "效果${index + 1}: 请输入有效的周期值", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
                1 -> {
                    if (effect.bonusPercent < 0 || effect.bonusPercent > 100) {
                        Toast.makeText(requireContext(), "效果${index + 1}: 加成百分比需在0-100之间", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
                else -> {
                    if (effect.bonusPercent < 0 || effect.bonusPercent > 100) {
                        Toast.makeText(requireContext(), "效果${index + 1}: 衰减百分比需在0-100之间", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
            }
        }
        
        if (dialogBinding.cbHasDuration.isChecked) {
            val durationValue = dialogBinding.etDurationValue.text.toString().trim()
            if (durationValue.isEmpty() || durationValue.toIntOrNull()?.let { it <= 0 } != false) {
                Toast.makeText(requireContext(), "请输入有效的持续时间", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        
        return true
    }

    private fun saveStatus(
        dialogBinding: DialogAddStatusMultiEffectBinding,
        effectsAdapter: StatusEffectAdapter
    ) {
        val name = dialogBinding.etName.text.toString().trim()
        val description = dialogBinding.etDescription.text.toString().trim()
        val colorHex = colorOptions[selectedColorIndex]
        
        val effectItems = effectsAdapter.getEffects()
        val effects = effectItems.mapIndexed { index, item ->
            item.toStatusEffectEntity(0L, index)
        }
        
        val durationValue: Int
        val durationUnit: Int
        if (dialogBinding.cbHasDuration.isChecked) {
            durationValue = dialogBinding.etDurationValue.text.toString().toInt()
            durationUnit = dialogBinding.spinnerDurationUnit.selectedItemPosition
        } else {
            durationValue = 0
            durationUnit = 0
        }
        
        if (editingStatus != null) {
            val updatedStatus = editingStatus!!.copy(
                name = name,
                description = description,
                colorHex = colorHex,
                durationValue = durationValue,
                durationUnit = durationUnit
            )
            statusViewModel.updateStatus(updatedStatus, effects)
            Toast.makeText(requireContext(), "状态已更新", Toast.LENGTH_SHORT).show()
        } else {
            statusViewModel.addStatus(
                name = name,
                description = description,
                colorHex = colorHex,
                effects = effects,
                durationValue = durationValue,
                durationUnit = durationUnit
            )
            Toast.makeText(requireContext(), "状态已创建", Toast.LENGTH_SHORT).show()
        }
        
        editingStatus = null
        editingEffects = null
    }
}
