package com.example.lifegame.ui.attribute

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.databinding.DialogAddStatusBinding
import com.example.lifegame.databinding.FragmentAttributeBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttributeFragment : BaseFragment<FragmentAttributeBinding>() {

    private val viewModel: AttributeViewModel by viewModels()
    private val statusViewModel: StatusViewModel by viewModels()
    private lateinit var attributeListFragment: AttributeListFragment
    private lateinit var statusPlaceholderFragment: StatusPlaceholderFragment
    private lateinit var pagerAdapter: InfoPagerAdapter

    private val colorOptions = listOf(
        "#21212B", "#1A237E", "#0D47A1", "#01579B",
        "#006064", "#004D40", "#1B5E20", "#33691E",
        "#827717", "#F57F17", "#FF6F00", "#E65100",
        "#BF360C", "#3E2723", "#4A148C", "#880E4F"
    )
    private var selectedColorIndex = 0
    private var editingStatus: StatusEntity? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeBinding {
        return FragmentAttributeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        
        attributeListFragment = AttributeListFragment()
        statusPlaceholderFragment = StatusPlaceholderFragment()
        
        pagerAdapter = InfoPagerAdapter(requireActivity(), attributeListFragment, statusPlaceholderFragment)
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
            if (binding.viewPager.currentItem == 0) {
                attributeListFragment.triggerSortMode()
                updateSortButton()
            } else {
                statusPlaceholderFragment.triggerSortMode()
                updateSortButtonForStatus()
            }
        }
    }

    private fun showAddMenu() {
        val items = if (binding.viewPager.currentItem == 0) {
            arrayOf("新增属性", "新增状态")
        } else {
            arrayOf("新增状态")
        }
        
        AlertDialog.Builder(requireContext(), R.style.Theme_LifeGame_Dialog)
            .setItems(items) { _, which ->
                if (binding.viewPager.currentItem == 0) {
                    when (which) {
                        0 -> attributeListFragment.triggerAddAttribute()
                        1 -> showAddStatusDialog(null)
                    }
                } else {
                    showAddStatusDialog(null)
                }
            }
            .show()
    }

    private fun updateSortButton() {
        if (attributeListFragment.isInSortMode()) {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_save)
            binding.btnAdd.visibility = View.GONE
        } else {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
        }
    }

    private fun updateSortButtonForStatus() {
        if (statusPlaceholderFragment.isInSortMode()) {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_save)
            binding.btnAdd.visibility = View.GONE
        } else {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
        }
    }

    fun showAddStatusDialog(status: StatusEntity?) {
        editingStatus = status
        val dialogBinding = DialogAddStatusBinding.inflate(LayoutInflater.from(requireContext()))
        
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_LifeGame_Dialog)
            .setView(dialogBinding.root)
            .create()
        
        setupColorPicker(dialogBinding)
        setupAttributeSpinner(dialogBinding)
        setupEffectTypeSwitcher(dialogBinding)
        setupPeriodUnitSpinner(dialogBinding)
        
        if (status != null) {
            dialogBinding.tvDialogTitle.text = "编辑状态"
            dialogBinding.etName.setText(status.name)
            dialogBinding.etDescription.setText(status.description)
            selectedColorIndex = colorOptions.indexOf(status.colorHex).takeIf { it >= 0 } ?: 0
            updateColorSelection(dialogBinding)
            
            if (status.effectType == 0) {
                dialogBinding.rbPeriodic.isChecked = true
                dialogBinding.etPeriodValue.setText(status.periodValue.toString())
                dialogBinding.spinnerPeriodUnit.setSelection(status.periodUnit)
                dialogBinding.etChangeValue.setText(status.changeValue.toString())
            } else {
                dialogBinding.rbBonus.isChecked = true
                dialogBinding.etBonusPercent.setText(status.bonusPercent.toString())
            }
        }
        
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        
        dialogBinding.btnConfirm.setOnClickListener {
            if (validateInput(dialogBinding)) {
                saveStatus(dialogBinding)
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun setupColorPicker(dialogBinding: DialogAddStatusBinding) {
        dialogBinding.rvColors.adapter = ColorPickerAdapter(colorOptions) { position ->
            selectedColorIndex = position
            updateColorSelection(dialogBinding)
        }
        updateColorSelection(dialogBinding)
    }

    private fun updateColorSelection(dialogBinding: DialogAddStatusBinding) {
        val adapter = dialogBinding.rvColors.adapter as? ColorPickerAdapter
        adapter?.selectedPosition = selectedColorIndex
        adapter?.notifyDataSetChanged()
    }

    private fun setupAttributeSpinner(dialogBinding: DialogAddStatusBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            statusViewModel.attributes.collect { attributes ->
                if (attributes.isNotEmpty()) {
                    val attrNames = attributes.map { it.attribute.name }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, attrNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    dialogBinding.spinnerAttribute.adapter = adapter
                    
                    editingStatus?.let { status ->
                        val index = attributes.indexOfFirst { it.attribute.id == status.targetAttributeId }
                        if (index >= 0) {
                            dialogBinding.spinnerAttribute.setSelection(index)
                        }
                    }
                }
            }
        }
    }

    private fun setupEffectTypeSwitcher(dialogBinding: DialogAddStatusBinding) {
        dialogBinding.rgEffectType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_periodic) {
                dialogBinding.llPeriodicParams.visibility = View.VISIBLE
                dialogBinding.llBonusParams.visibility = View.GONE
            } else {
                dialogBinding.llPeriodicParams.visibility = View.GONE
                dialogBinding.llBonusParams.visibility = View.VISIBLE
            }
        }
    }

    private fun setupPeriodUnitSpinner(dialogBinding: DialogAddStatusBinding) {
        val units = arrayOf("小时", "天")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPeriodUnit.adapter = adapter
    }

    private fun validateInput(dialogBinding: DialogAddStatusBinding): Boolean {
        val name = dialogBinding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请输入状态名称", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val attributes = statusViewModel.attributes.value
        if (attributes.isEmpty()) {
            Toast.makeText(requireContext(), "请先创建属性", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (dialogBinding.rbPeriodic.isChecked) {
            val periodValue = dialogBinding.etPeriodValue.text.toString().trim()
            if (periodValue.isEmpty() || periodValue.toIntOrNull()?.let { it <= 0 } != false) {
                Toast.makeText(requireContext(), "请输入有效的周期值", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val changeValue = dialogBinding.etChangeValue.text.toString().trim()
            if (changeValue.isEmpty() || changeValue.toFloatOrNull() == null) {
                Toast.makeText(requireContext(), "请输入有效的变动值", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            val bonusPercent = dialogBinding.etBonusPercent.text.toString().trim()
            if (bonusPercent.isEmpty() || bonusPercent.toFloatOrNull()?.let { it < 0 || it > 100 } != false) {
                Toast.makeText(requireContext(), "请输入0-100之间的加成百分比", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        
        return true
    }

    private fun saveStatus(dialogBinding: DialogAddStatusBinding) {
        val name = dialogBinding.etName.text.toString().trim()
        val description = dialogBinding.etDescription.text.toString().trim()
        val colorHex = colorOptions[selectedColorIndex]
        val effectType = if (dialogBinding.rbPeriodic.isChecked) 0 else 1
        val attributes = statusViewModel.attributes.value
        val selectedAttrIndex = dialogBinding.spinnerAttribute.selectedItemPosition
        val targetAttributeId = attributes[selectedAttrIndex].attribute.id
        
        if (editingStatus != null) {
            val updatedStatus = editingStatus!!.copy(
                name = name,
                description = description,
                colorHex = colorHex,
                effectType = effectType,
                targetAttributeId = targetAttributeId,
                periodValue = if (effectType == 0) dialogBinding.etPeriodValue.text.toString().toInt() else 0,
                periodUnit = if (effectType == 0) dialogBinding.spinnerPeriodUnit.selectedItemPosition else 0,
                changeValue = if (effectType == 0) dialogBinding.etChangeValue.text.toString().toFloat() else 0f,
                bonusPercent = if (effectType == 1) dialogBinding.etBonusPercent.text.toString().toFloat() else 0f
            )
            statusViewModel.updateStatus(updatedStatus)
            Toast.makeText(requireContext(), "状态已更新", Toast.LENGTH_SHORT).show()
        } else {
            statusViewModel.addStatus(
                name = name,
                description = description,
                colorHex = colorHex,
                effectType = effectType,
                targetAttributeId = targetAttributeId,
                periodValue = if (effectType == 0) dialogBinding.etPeriodValue.text.toString().toInt() else 0,
                periodUnit = if (effectType == 0) dialogBinding.spinnerPeriodUnit.selectedItemPosition else 0,
                changeValue = if (effectType == 0) dialogBinding.etChangeValue.text.toString().toFloat() else 0f,
                bonusPercent = if (effectType == 1) dialogBinding.etBonusPercent.text.toString().toFloat() else 0f
            )
            Toast.makeText(requireContext(), "状态已创建", Toast.LENGTH_SHORT).show()
        }
        
        editingStatus = null
    }
}
