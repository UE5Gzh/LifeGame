package com.example.lifegame.ui.behavior

import android.content.Intent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.BehaviorGroupEntity
import com.example.lifegame.databinding.DialogAddBehaviorBinding
import com.example.lifegame.databinding.DialogAddGroupBinding
import com.example.lifegame.databinding.DialogManageGroupsBinding
import com.example.lifegame.databinding.DialogResetEnergyBinding
import com.example.lifegame.databinding.DialogSetMaxEnergyBinding
import com.example.lifegame.databinding.FragmentBehaviorBinding
import com.example.lifegame.databinding.ItemBehaviorAttributeModifierBinding
import com.example.lifegame.ui.base.BaseFragment
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class BehaviorFragment : BaseFragment<FragmentBehaviorBinding>() {

    private val viewModel: BehaviorViewModel by viewModels()
    private lateinit var adapter: BehaviorAdapter
    private var isSortMode = false
    private var itemTouchHelper: ItemTouchHelper? = null
    private var currentSelectedGroupId: Long? = null
    private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null

    private val focusLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val behaviorId = result.data?.getLongExtra("behavior_id", -1L) ?: -1L
            if (behaviorId != -1L) {
                val behavior = viewModel.behaviors.value.find { it.behavior.id == behaviorId }
                if (behavior != null) {
                    viewModel.executeBehavior(behavior, isFocus = true)
                }
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBehaviorBinding {
        return FragmentBehaviorBinding.inflate(inflater, container, false)
    }

    private fun showBehaviorOptionsDialog(behaviorWithModifiers: BehaviorWithModifiers) {
        val options = arrayOf("移动到分组...", "删除行动")
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle(behaviorWithModifiers.behavior.name)
            .setItems(options) { _, which ->
                when (val option = options[which]) {
                    "移动到分组..." -> {
                        showMoveToGroupDialog(behaviorWithModifiers)
                    }
                    "删除行动" -> {
                        showDeleteBehaviorDialog(behaviorWithModifiers)
                    }
                }
            }
            .show()
    }

    private fun showMoveToGroupDialog(behaviorWithModifiers: BehaviorWithModifiers) {
        val availableGroups = viewModel.behaviorGroups.value
        val groupNames = mutableListOf("未分组")
        groupNames.addAll(availableGroups.map { it.name })

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("移动到分组")
            .setItems(groupNames.toTypedArray()) { _, which ->
                val groupId = if (which > 0) availableGroups[which - 1].id else null
                
                // Copy modifiers
                val modifiers = behaviorWithModifiers.modifiers.map { Pair(it.attributeId, it.valueChange) }
                val behavior = behaviorWithModifiers.behavior
                
                viewModel.updateBehavior(
                    behavior.id,
                    behavior.name,
                    behavior.energyType,
                    behavior.energyValue,
                    behavior.focusDuration,
                    groupId,
                    modifiers
                )
            }
            .show()
    }

    private fun showDeleteBehaviorDialog(behaviorWithModifiers: BehaviorWithModifiers) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("删除行动")
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
        dialogBinding.tvDialogTitle.text = "修改行动"

        val availableGroups = viewModel.behaviorGroups.value
        val groupNames = mutableListOf("未分组")
        groupNames.addAll(availableGroups.map { it.name })
        
        val groupAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            groupNames
        )
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerGroup.adapter = groupAdapter

        val behavior = behaviorWithModifiers.behavior
        
        // set group selection
        if (behavior.groupId != null) {
            val index = availableGroups.indexOfFirst { it.id == behavior.groupId }
            if (index >= 0) {
                dialogBinding.spinnerGroup.setSelection(index + 1) // +1 because "未分组" is at 0
            }
        } else {
            dialogBinding.spinnerGroup.setSelection(0)
        }

        dialogBinding.etName.setText(behavior.name)
        if (behavior.energyType == 0) {
            dialogBinding.rbConsume.isChecked = true
        } else {
            dialogBinding.rbRestore.isChecked = true
        }
        dialogBinding.etEnergyValue.setText(behavior.energyValue.toString())
        dialogBinding.etFocusDuration.setText(behavior.focusDuration.toString())

        val availableAttributes = viewModel.attributes.value

        fun addAttributeRow(preSelectedAttributeId: Long? = null, preValueChange: Float = 1f) {
            if (availableAttributes.isEmpty()) {
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
            
            rowBinding.etValueChange.setText(if (preValueChange == preValueChange.toInt().toFloat()) preValueChange.toInt().toString() else preValueChange.toString())

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
                return@setOnClickListener
            }

            val energyValue = energyValueStr?.toIntOrNull() ?: 10
            val focusDuration = focusDurationStr?.toIntOrNull() ?: 0
            
            val selectedGroupPos = dialogBinding.spinnerGroup.selectedItemPosition
            val groupId = if (selectedGroupPos > 0) availableGroups[selectedGroupPos - 1].id else null

            val modifiers = mutableListOf<Pair<Long, Float>>()
            for (i in 0 until dialogBinding.llAttributesContainer.childCount) {
                val child = dialogBinding.llAttributesContainer.getChildAt(i)
                val rowBinding = ItemBehaviorAttributeModifierBinding.bind(child)
                
                val selectedIndex = rowBinding.spinnerAttribute.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < availableAttributes.size) {
                    val attributeId = availableAttributes[selectedIndex].attribute.id
                    val valueChangeStr = rowBinding.etValueChange.text?.toString()?.trim()
                    val valueChange = valueChangeStr?.toFloatOrNull() ?: 0f
                    if (valueChange != 0f) {
                        modifiers.add(Pair(attributeId, valueChange))
                    }
                }
            }

            viewModel.updateBehavior(behavior.id, name, energyType, energyValue, focusDuration, groupId, modifiers)
            dialog.dismiss()
        }

        dialog.show()
    }
    override fun setupViews() {
        super.setupViews()
        
        setupRecyclerView()
        setupGestureDetector()

        binding.btnAdd.setOnClickListener {
            showAddBehaviorDialog()
        }

        binding.btnResetEnergy.setOnClickListener {
            val dialogBinding = DialogResetEnergyBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialogBinding.btnConfirm.setOnClickListener {
                viewModel.resetEnergy()
                dialog.dismiss()
            }

            dialog.show()
        }

        binding.cardEnergy.setOnLongClickListener {
            showSetMaxEnergyDialog()
            true
        }

        binding.btnList.setOnClickListener {
            toggleSortMode()
        }

        binding.btnGroupManage.setOnClickListener {
            showManageGroupsDialog()
        }

        setupTabLayout()
    }

    private fun setupTabLayout() {
        currentSelectedGroupId = viewModel.selectedGroupId.value
        
        tabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentSelectedGroupId = tab?.tag as? Long
                viewModel.saveSelectedGroupId(currentSelectedGroupId)
                filterBehaviors()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        }
        
        binding.tabGroups.addOnTabSelectedListener(tabSelectedListener!!)
    }

    private fun filterBehaviors() {
        val allBehaviors = viewModel.behaviors.value
        val filtered = if (currentSelectedGroupId == null) {
            allBehaviors
        } else if (currentSelectedGroupId == -1L) {
            // Uncategorized
            allBehaviors.filter { it.behavior.groupId == null }
        } else {
            allBehaviors.filter { it.behavior.groupId == currentSelectedGroupId }
        }
        adapter.submitList(filtered)
    }

    private fun updateTabs(groups: List<BehaviorGroupEntity>, behaviors: List<BehaviorWithModifiers>) {
        tabSelectedListener?.let { binding.tabGroups.removeOnTabSelectedListener(it) }
        
        binding.tabGroups.removeAllTabs()
        
        val allTab = binding.tabGroups.newTab()
        allTab.text = "全部 (${behaviors.size})"
        allTab.tag = null
        binding.tabGroups.addTab(allTab)

        for (group in groups) {
            val count = behaviors.count { it.behavior.groupId == group.id }
            val tab = binding.tabGroups.newTab()
            tab.text = "${group.name} ($count)"
            tab.tag = group.id
            binding.tabGroups.addTab(tab)
        }

        val uncategorizedCount = behaviors.count { it.behavior.groupId == null }
        if (uncategorizedCount > 0) {
            val uncatTab = binding.tabGroups.newTab()
            uncatTab.text = "未分组 ($uncategorizedCount)"
            uncatTab.tag = -1L
            binding.tabGroups.addTab(uncatTab)
        }

        val savedGroupId = viewModel.selectedGroupId.value
        val tabCount = binding.tabGroups.tabCount
        var found = false
        
        for (i in 0 until tabCount) {
            val tab = binding.tabGroups.getTabAt(i)
            if (tab != null && tab.tag == savedGroupId) {
                tab.select()
                currentSelectedGroupId = savedGroupId
                found = true
                break
            }
        }
        
        if (!found) {
            binding.tabGroups.getTabAt(0)?.select()
            currentSelectedGroupId = null
            viewModel.saveSelectedGroupId(null)
        }
        
        tabSelectedListener?.let { binding.tabGroups.addOnTabSelectedListener(it) }
    }

    private fun toggleSortMode() {
        isSortMode = !isSortMode
        adapter.isSortMode = isSortMode
        if (isSortMode) {
            binding.btnList.setImageResource(android.R.drawable.ic_menu_save)
            binding.btnAdd.visibility = View.GONE
            itemTouchHelper?.attachToRecyclerView(binding.rvBehaviors)
        } else {
            binding.btnList.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
            itemTouchHelper?.attachToRecyclerView(null)
            saveSortOrder()
        }
    }

    private fun saveSortOrder() {
        val currentList = adapter.currentList
        val updatedBehaviors = currentList.mapIndexed { index, behaviorWithModifiers ->
            behaviorWithModifiers.behavior.copy(sortOrder = index)
        }
        viewModel.updateBehaviorSortOrders(updatedBehaviors)
    }

    private fun showSetMaxEnergyDialog() {
        val dialogBinding = DialogSetMaxEnergyBinding.inflate(layoutInflater)
        dialogBinding.etEnergy.setText(viewModel.maxEnergy.value.toString())
        dialogBinding.etEnergy.setSelection(dialogBinding.etEnergy.text?.length ?: 0)

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val newMax = dialogBinding.etEnergy.text.toString().toIntOrNull()
            if (newMax != null && newMax > 0) {
                viewModel.setMaxEnergy(newMax)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = BehaviorAdapter(
            onActionClick = { behavior, onComplete ->
                if (!isSortMode) {
                    if (behavior.behavior.focusDuration > 0) {
                        val intent = Intent(requireContext(), com.example.lifegame.ui.focus.FocusActivity::class.java).apply {
                            putExtra("behavior_id", behavior.behavior.id)
                            putExtra("behavior_name", behavior.behavior.name)
                            putExtra("focus_duration", behavior.behavior.focusDuration)
                        }
                        focusLauncher.launch(intent)
                        onComplete()
                    } else {
                        binding.rvBehaviors.postDelayed({
                            viewModel.executeBehavior(behavior)
                            onComplete()
                        }, 600)
                    }
                } else {
                    onComplete()
                }
            },
            onItemClick = { behavior ->
                if (!isSortMode) {
                    showEditBehaviorDialog(behavior)
                }
            },
            onItemLongClick = { behavior ->
                if (!isSortMode) {
                    showBehaviorOptionsDialog(behavior)
                }
            }
        )
        binding.rvBehaviors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBehaviors.adapter = adapter

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

    private lateinit var gestureDetector: GestureDetector

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    val tabCount = binding.tabGroups.tabCount
                    val currentTab = binding.tabGroups.selectedTabPosition
                    
                    if (diffX > 0 && currentTab > 0) {
                        binding.tabGroups.getTabAt(currentTab - 1)?.select()
                        return true
                    } else if (diffX < 0 && currentTab < tabCount - 1) {
                        binding.tabGroups.getTabAt(currentTab + 1)?.select()
                        return true
                    }
                }
                return false
            }
        })
        
        binding.rvBehaviors.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
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
                        updateTabs(viewModel.behaviorGroups.value, behaviors)
                        filterBehaviors()
                    }
                }
                launch {
                    viewModel.behaviorGroups.collect { groups ->
                        updateTabs(groups, viewModel.behaviors.value)
                        filterBehaviors()
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

    private fun showManageGroupsDialog() {
        val dialogBinding = DialogManageGroupsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)

        val groupAdapter = BehaviorGroupAdapter(
            onEditClick = { group ->
                showEditGroupDialog(group)
            },
            onDeleteClick = { group ->
                MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle("删除分组")
                    .setMessage("确定要删除分组「${group.name}」吗？组内行动将被移至未分组。")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteGroup(group)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )
        dialogBinding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvGroups.adapter = groupAdapter

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
                groupAdapter.swapItems(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(touchHelperCallback).attachToRecyclerView(dialogBinding.rvGroups)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.behaviorGroups.collect { groups ->
                    groupAdapter.submitList(groups)
                }
            }
        }

        dialogBinding.btnAddGroup.setOnClickListener {
            showAddGroupDialog()
        }

        dialogBinding.btnClose.setOnClickListener {
            // Save sort order
            val updatedGroups = groupAdapter.currentList.mapIndexed { index, group ->
                group.copy(sortOrder = index)
            }
            viewModel.updateGroupSortOrders(updatedGroups)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddGroupDialog() {
        val dialogBinding = DialogAddGroupBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etGroupName.text?.toString()?.trim()
            if (!name.isNullOrEmpty()) {
                viewModel.addGroup(name)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showEditGroupDialog(group: BehaviorGroupEntity) {
        val dialogBinding = DialogAddGroupBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvTitle.text = "修改分组"
        dialogBinding.etGroupName.setText(group.name)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etGroupName.text?.toString()?.trim()
            if (!name.isNullOrEmpty()) {
                viewModel.updateGroup(group.copy(name = name))
                dialog.dismiss()
            }
        }
        dialog.show()
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
        val availableGroups = viewModel.behaviorGroups.value
        
        val groupNames = mutableListOf("未分组")
        groupNames.addAll(availableGroups.map { it.name })
        
        val groupAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            groupNames
        )
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerGroup.adapter = groupAdapter

        // set default group to current selected if not "All"
        if (currentSelectedGroupId != null) {
            val index = availableGroups.indexOfFirst { it.id == currentSelectedGroupId }
            if (index >= 0) {
                dialogBinding.spinnerGroup.setSelection(index + 1)
            }
        }

        // Helper to add an attribute modifier row
        fun addAttributeRow() {
            if (availableAttributes.isEmpty()) {
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
                return@setOnClickListener
            }

            val energyValue = energyValueStr?.toIntOrNull() ?: 10
            val focusDuration = focusDurationStr?.toIntOrNull() ?: 0

            val selectedGroupPos = dialogBinding.spinnerGroup.selectedItemPosition
            val groupId = if (selectedGroupPos > 0) availableGroups[selectedGroupPos - 1].id else null

            val modifiers = mutableListOf<Pair<Long, Float>>()
            for (i in 0 until dialogBinding.llAttributesContainer.childCount) {
                val child = dialogBinding.llAttributesContainer.getChildAt(i)
                val rowBinding = ItemBehaviorAttributeModifierBinding.bind(child)
                
                val selectedIndex = rowBinding.spinnerAttribute.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < availableAttributes.size) {
                    val attributeId = availableAttributes[selectedIndex].attribute.id
                    val valueChangeStr = rowBinding.etValueChange.text?.toString()?.trim()
                    val valueChange = valueChangeStr?.toFloatOrNull() ?: 0f
                    if (valueChange != 0f) {
                        modifiers.add(Pair(attributeId, valueChange))
                    }
                }
            }

            viewModel.addBehavior(name, energyType, energyValue, focusDuration, groupId, modifiers)
            dialog.dismiss()
        }

        dialog.show()
    }
}
