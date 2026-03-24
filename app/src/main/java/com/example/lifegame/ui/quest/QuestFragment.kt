package com.example.lifegame.ui.quest

import android.app.DatePickerDialog
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.databinding.DialogCreateQuestBinding
import com.example.lifegame.data.entity.QuestEntity
import com.example.lifegame.databinding.DialogClaimRewardBinding
import com.example.lifegame.databinding.DialogConfirmBinding
import com.example.lifegame.databinding.DialogQuestDetailBinding
import com.example.lifegame.databinding.DialogQuestOptionsBinding
import com.example.lifegame.databinding.FragmentQuestBinding
import com.example.lifegame.databinding.ItemQuestAttrGoalBinding
import com.example.lifegame.databinding.ItemQuestBehGoalBinding
import com.example.lifegame.databinding.ItemQuestEffectBinding
import com.example.lifegame.databinding.ItemQuestGoalBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

@AndroidEntryPoint
class QuestFragment : BaseFragment<FragmentQuestBinding>() {

    private val viewModel: QuestViewModel by viewModels()
    private lateinit var adapter: QuestAdapter
    private var currentSelectedTabType: Int = 0
    private var isSortMode = false
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentQuestBinding {
        return FragmentQuestBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()

        setupRecyclerView()
        setupTabLayout()
        setupGestureDetector()

        binding.btnAdd.setOnClickListener {
            if (!isSortMode) {
                showCreateQuestDialog()
            }
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
            itemTouchHelper?.attachToRecyclerView(binding.rvQuests)
        } else {
            binding.btnSort.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            binding.btnAdd.visibility = View.VISIBLE
            itemTouchHelper?.attachToRecyclerView(null)
            saveSortOrder()
        }
    }

    private fun saveSortOrder() {
        val currentList = adapter.currentList
        val updatedQuests = currentList.mapIndexed { index, questWithDetails ->
            questWithDetails.quest.copy(sortOrder = index)
        }
        viewModel.updateQuestSortOrders(updatedQuests)
    }

    private fun setupRecyclerView() {
        adapter = QuestAdapter(
            onQuestClick = { quest ->
                if (!isSortMode) {
                    handleQuestClick(quest)
                }
            },
            onQuestLongClick = { quest ->
                if (!isSortMode) {
                    showQuestOptionsDialog(quest)
                }
            },
            calculateProgress = { quest ->
                viewModel.calculateProgress(quest, viewModel.attributes.value)
            }
        )
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.adapter = adapter

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
                    val tabCount = binding.tabQuestType.tabCount
                    val currentTab = binding.tabQuestType.selectedTabPosition
                    
                    if (diffX > 0 && currentTab > 0) {
                        binding.tabQuestType.getTabAt(currentTab - 1)?.select()
                        return true
                    } else if (diffX < 0 && currentTab < tabCount - 1) {
                        binding.tabQuestType.getTabAt(currentTab + 1)?.select()
                        return true
                    }
                }
                return false
            }
        })
        
        binding.rvQuests.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupTabLayout() {
        currentSelectedTabType = viewModel.selectedTabType.value
        binding.tabQuestType.selectTab(binding.tabQuestType.getTabAt(currentSelectedTabType))
        binding.tabQuestType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentSelectedTabType = tab?.position ?: 0
                viewModel.saveSelectedTabType(currentSelectedTabType)
                filterQuests()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun filterQuests() {
        val allQuests = viewModel.quests.value
        val type = when (currentSelectedTabType) {
            0 -> 0
            1 -> 3
            2 -> 1
            3 -> 2
            else -> 0
        }
        val filtered = allQuests.filter { it.quest.type == type }
        adapter.submitList(filtered)
    }

    override fun observeData() {
        super.observeData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.attributes.collect {
                        adapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.quests.collect {
                        filterQuests()
                    }
                }
                launch {
                    viewModel.behaviors.collect {
                    }
                }
            }
        }
    }

    private fun handleQuestClick(questWithDetails: QuestWithDetails) {
        val quest = questWithDetails.quest
        when (quest.status) {
            0 -> {
                showQuestDetailsDialog(questWithDetails)
            }
            1 -> {
                showClaimRewardDialog(questWithDetails)
            }
            2 -> {
                showQuestDetailsDialog(questWithDetails)
            }
            3 -> {
                showQuestDetailsDialog(questWithDetails)
            }
        }
    }

    private fun showQuestOptionsDialog(questWithDetails: QuestWithDetails) {
        val quest = questWithDetails.quest
        val dialogBinding = DialogQuestOptionsBinding.inflate(layoutInflater)
        
        dialogBinding.btnInstantComplete.visibility = if (quest.status == 0) View.VISIBLE else View.GONE
        dialogBinding.btnGiveUp.visibility = if (quest.status == 0) View.VISIBLE else View.GONE
        dialogBinding.btnEdit.visibility = if (quest.status == 0) View.VISIBLE else View.GONE
        dialogBinding.btnFocus.visibility = if (quest.status == 0) View.VISIBLE else View.GONE
        dialogBinding.btnFocus.text = if (quest.isFocused) "取消关注" else "设为关注任务"
        dialogBinding.btnDelete.visibility = View.VISIBLE

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnInstantComplete.setOnClickListener {
            showInstantCompleteConfirmDialog(questWithDetails)
            dialog.dismiss()
        }

        dialogBinding.btnGiveUp.setOnClickListener {
            showGiveUpConfirmDialog(questWithDetails)
            dialog.dismiss()
        }

        dialogBinding.btnEdit.setOnClickListener {
            showEditQuestDialog(questWithDetails)
            dialog.dismiss()
        }

        dialogBinding.btnFocus.setOnClickListener {
            viewModel.toggleQuestFocus(quest)
            dialog.dismiss()
        }

        dialogBinding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog(quest)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmDialog(quest: QuestEntity) {
        val dialogBinding = DialogConfirmBinding.inflate(layoutInflater)
        dialogBinding.tvTitle.text = "确认删除"
        dialogBinding.tvMessage.text = "确定要彻底删除该任务吗？该操作不可恢复。"

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.deleteQuest(quest)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showGiveUpConfirmDialog(questWithDetails: QuestWithDetails) {
        val dialogBinding = DialogConfirmBinding.inflate(layoutInflater)
        dialogBinding.tvTitle.text = "确认放弃"
        dialogBinding.tvMessage.text = "放弃任务将触发该任务的惩罚（如果有），确定放弃吗？"

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.giveUpQuest(questWithDetails)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showInstantCompleteConfirmDialog(questWithDetails: QuestWithDetails) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("确认立即完成")
            .setMessage("确认立即完成任务「${questWithDetails.quest.name}」吗？\n\n注意：这将跳过目标达成过程，直接获得奖励。\n任务惩罚将不会被触发。")
            .setPositiveButton("确认完成") { _, _ ->
                viewModel.instantCompleteQuest(questWithDetails)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showQuestDetailsDialog(q: QuestWithDetails) {
        val dialogBinding = DialogQuestDetailBinding.inflate(layoutInflater)
        
        dialogBinding.tvQuestName.text = q.quest.name
        
        val typeText = when (q.quest.type) {
            0 -> "日常"
            1 -> "主线"
            2 -> "支线"
            3 -> "周常"
            else -> "任务"
        }
        dialogBinding.tvQuestType.text = typeText

        dialogBinding.llGoalsContainer.removeAllViews()
        q.attributeGoals.forEach { ag ->
            val goalBinding = ItemQuestGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val attr = viewModel.attributes.value.find { it.attribute.id == ag.attributeId }?.attribute
            val attrName = attr?.name ?: "未知属性"
            val currentVal = attr?.currentValue ?: 0f
            val targetVal = ag.targetValue
            val progress = if (targetVal > 0) ((currentVal / targetVal) * 100).toInt().coerceAtMost(100) else 100
            
            goalBinding.tvGoalName.text = attrName
            goalBinding.tvGoalProgressText.text = "${currentVal.toInt()}/${targetVal.toInt()}"
            goalBinding.pbGoalProgress.progress = progress
            goalBinding.tvGoalPercent.text = "$progress%"
        }
        q.behaviorGoals.forEach { bg ->
            val goalBinding = ItemQuestGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val behName = viewModel.behaviors.value.find { it.behavior.id == bg.behaviorId }?.behavior?.name ?: "未知行动"
            val targetCount = bg.targetCount
            val currentCount = bg.currentCount
            val progress = if (targetCount > 0) ((currentCount.toFloat() / targetCount) * 100).toInt().coerceAtMost(100) else 100
            
            goalBinding.tvGoalName.text = behName
            goalBinding.tvGoalProgressText.text = "$currentCount/$targetCount 次"
            goalBinding.pbGoalProgress.progress = progress
            goalBinding.tvGoalPercent.text = "$progress%"
        }

        dialogBinding.llRewardsContainer.removeAllViews()
        val rewards = q.effects.filter { !it.isPunishment }
        if (rewards.isEmpty()) {
            val tv = android.widget.TextView(requireContext())
            tv.text = "无奖励"
            tv.setTextColor(android.graphics.Color.parseColor("#888888"))
            tv.textSize = 14f
            dialogBinding.llRewardsContainer.addView(tv)
        } else {
            rewards.forEach { e ->
                val tv = android.widget.TextView(requireContext())
                if (e.type == 0) {
                    val attrName = viewModel.attributes.value.find { it.attribute.id == e.attributeId }?.attribute?.name ?: "未知属性"
                    val sign = if ((e.valueChange ?: 0f) >= 0f) "+" else ""
                    tv.text = "• $attrName $sign${e.valueChange}"
                } else {
                    tv.text = "• ${e.text}"
                }
                tv.setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
                tv.textSize = 14f
                dialogBinding.llRewardsContainer.addView(tv)
            }
        }

        val punishments = q.effects.filter { it.isPunishment }
        if (punishments.isNotEmpty()) {
            dialogBinding.sectionPunishments.visibility = View.VISIBLE
            dialogBinding.llPunishmentsContainer.removeAllViews()
            punishments.forEach { e ->
                val tv = android.widget.TextView(requireContext())
                if (e.type == 0) {
                    val attrName = viewModel.attributes.value.find { it.attribute.id == e.attributeId }?.attribute?.name ?: "未知属性"
                    val sign = if ((e.valueChange ?: 0f) >= 0f) "+" else ""
                    tv.text = "• $attrName $sign${e.valueChange}"
                } else {
                    tv.text = "• ${e.text}"
                }
                tv.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
                tv.textSize = 14f
                dialogBinding.llPunishmentsContainer.addView(tv)
            }
        } else {
            dialogBinding.sectionPunishments.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showClaimRewardDialog(questWithDetails: QuestWithDetails) {
        val dialogBinding = DialogClaimRewardBinding.inflate(layoutInflater)
        val quest = questWithDetails.quest
        
        dialogBinding.tvQuestName.text = "「${quest.name}」"

        dialogBinding.llRewardsContainer.removeAllViews()
        val rewards = questWithDetails.effects.filter { !it.isPunishment }
        if (rewards.isEmpty()) {
            val tv = android.widget.TextView(requireContext())
            tv.text = "无奖励"
            tv.setTextColor(android.graphics.Color.parseColor("#888888"))
            tv.textSize = 14f
            dialogBinding.llRewardsContainer.addView(tv)
        } else {
            rewards.forEach { e ->
                val tv = android.widget.TextView(requireContext())
                if (e.type == 0) {
                    val attrName = viewModel.attributes.value.find { it.attribute.id == e.attributeId }?.attribute?.name ?: "未知属性"
                    val sign = if ((e.valueChange ?: 0f) >= 0f) "+" else ""
                    tv.text = "• $attrName $sign${e.valueChange}"
                } else {
                    tv.text = "• ${e.text}"
                }
                tv.setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
                tv.textSize = 14f
                dialogBinding.llRewardsContainer.addView(tv)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnClaim.setOnClickListener {
            viewModel.claimReward(questWithDetails)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCreateQuestDialog() {
        val dialogBinding = DialogCreateQuestBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)

        val availableAttributes = viewModel.attributes.value
        val availableBehaviors = viewModel.behaviors.value

        var selectedDeadline: Long? = null

        dialogBinding.spinnerType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0 || position == 1) {
                    dialogBinding.tvDeadlineLabel.visibility = View.GONE
                    dialogBinding.tvDeadline.visibility = View.GONE
                    selectedDeadline = null
                } else {
                    dialogBinding.tvDeadlineLabel.visibility = View.VISIBLE
                    dialogBinding.tvDeadline.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        dialogBinding.tvDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth, 23, 59, 59)
                    selectedDeadline = calendar.timeInMillis
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    dialogBinding.tvDeadline.text = format.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnAddAttrGoal.setOnClickListener {
            if (availableAttributes.isEmpty()) {
                return@setOnClickListener
            }
            val rowBinding = ItemQuestAttrGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val attrAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableAttributes.map { it.attribute.name })
            attrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerAttr.adapter = attrAdapter
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "attr"
        }

        dialogBinding.btnAddBehGoal.setOnClickListener {
            if (availableBehaviors.isEmpty()) {
                return@setOnClickListener
            }
            val rowBinding = ItemQuestBehGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val behAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableBehaviors.map { it.behavior.name })
            behAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerBeh.adapter = behAdapter
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "beh"
        }

        fun addEffectRow(container: ViewGroup, isPunishment: Boolean) {
            val rowBinding = ItemQuestEffectBinding.inflate(layoutInflater, container, true)
            
            val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("属性变动", "文本描述"))
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerType.adapter = typeAdapter

            if (availableAttributes.isNotEmpty()) {
                val attrAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableAttributes.map { it.attribute.name })
                attrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                rowBinding.spinnerAttr.adapter = attrAdapter
            }

            rowBinding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        rowBinding.llAttrChange.visibility = View.VISIBLE
                        rowBinding.tilText.visibility = View.GONE
                    } else {
                        rowBinding.llAttrChange.visibility = View.GONE
                        rowBinding.tilText.visibility = View.VISIBLE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            rowBinding.btnRemove.setOnClickListener { container.removeView(rowBinding.root) }
            rowBinding.root.tag = isPunishment
        }

        dialogBinding.btnAddReward.setOnClickListener { addEffectRow(dialogBinding.llRewardsContainer, false) }
        dialogBinding.btnAddPunishment.setOnClickListener { addEffectRow(dialogBinding.llPunishmentsContainer, true) }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val type = when (dialogBinding.spinnerType.selectedItemPosition) {
                0 -> 0
                1 -> 3
                2 -> 1
                else -> 2
            }

            if (type != 0 && type != 3 && selectedDeadline == null) {
                return@setOnClickListener
            }

            val attrGoals = mutableListOf<QuestAttributeGoalEntity>()
            val behGoals = mutableListOf<QuestBehaviorGoalEntity>()

            for (i in 0 until dialogBinding.llGoalsContainer.childCount) {
                val view = dialogBinding.llGoalsContainer.getChildAt(i)
                if (view.tag == "attr") {
                    val rb = ItemQuestAttrGoalBinding.bind(view)
                    val pos = rb.spinnerAttr.selectedItemPosition
                    val target = rb.etTargetVal.text.toString().toFloatOrNull()
                    if (pos >= 0 && target != null && target > 0f) {
                        attrGoals.add(QuestAttributeGoalEntity(questId = 0, attributeId = availableAttributes[pos].attribute.id, targetValue = target))
                    }
                } else if (view.tag == "beh") {
                    val rb = ItemQuestBehGoalBinding.bind(view)
                    val pos = rb.spinnerBeh.selectedItemPosition
                    val target = rb.etTargetCount.text.toString().toIntOrNull()
                    if (pos >= 0 && target != null && target > 0) {
                        behGoals.add(QuestBehaviorGoalEntity(questId = 0, behaviorId = availableBehaviors[pos].behavior.id, targetCount = target))
                    }
                }
            }

            if (attrGoals.isEmpty() && behGoals.isEmpty()) {
                return@setOnClickListener
            }

            val effects = mutableListOf<QuestEffectEntity>()
            
            fun parseEffectsContainer(container: ViewGroup) {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    val isPunish = view.tag as Boolean
                    val rb = ItemQuestEffectBinding.bind(view)
                    val effectType = rb.spinnerType.selectedItemPosition
                    
                    if (effectType == 0) {
                        val pos = rb.spinnerAttr.selectedItemPosition
                        val change = rb.etValChange.text.toString().toFloatOrNull()
                        if (pos >= 0 && change != null && availableAttributes.isNotEmpty()) {
                            effects.add(QuestEffectEntity(questId = 0, isPunishment = isPunish, type = 0, attributeId = availableAttributes[pos].attribute.id, valueChange = change))
                        }
                    } else {
                        val text = rb.etTextDesc.text?.toString()?.trim()
                        if (!text.isNullOrEmpty()) {
                            effects.add(QuestEffectEntity(questId = 0, isPunishment = isPunish, type = 1, text = text))
                        }
                    }
                }
            }
            
            parseEffectsContainer(dialogBinding.llRewardsContainer)
            parseEffectsContainer(dialogBinding.llPunishmentsContainer)

            viewModel.createQuest(name, type, selectedDeadline, attrGoals, behGoals, effects)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditQuestDialog(questWithDetails: QuestWithDetails) {
        val quest = questWithDetails.quest
        val dialogBinding = DialogCreateQuestBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)

        dialogBinding.etName.setText(quest.name)
        dialogBinding.spinnerType.setSelection(when (quest.type) {
            0 -> 0
            3 -> 1
            1 -> 2
            else -> 3
        })

        val availableAttributes = viewModel.attributes.value
        val availableBehaviors = viewModel.behaviors.value

        var selectedDeadline: Long? = quest.deadline

        if (quest.deadline != null) {
            dialogBinding.tvDeadlineLabel.visibility = View.VISIBLE
            dialogBinding.tvDeadline.visibility = View.VISIBLE
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            dialogBinding.tvDeadline.text = format.format(java.util.Date(quest.deadline!!))
        }

        dialogBinding.spinnerType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0 || position == 1) {
                    dialogBinding.tvDeadlineLabel.visibility = View.GONE
                    dialogBinding.tvDeadline.visibility = View.GONE
                    selectedDeadline = null
                } else {
                    dialogBinding.tvDeadlineLabel.visibility = View.VISIBLE
                    dialogBinding.tvDeadline.visibility = View.VISIBLE
                    if (quest.deadline != null) {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        dialogBinding.tvDeadline.text = format.format(java.util.Date(quest.deadline!!))
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        dialogBinding.tvDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth, 23, 59, 59)
                    selectedDeadline = calendar.timeInMillis
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    dialogBinding.tvDeadline.text = format.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        questWithDetails.attributeGoals.forEach { goal ->
            val rowBinding = ItemQuestAttrGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val attrAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableAttributes.map { it.attribute.name })
            attrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerAttr.adapter = attrAdapter
            val attrIndex = availableAttributes.indexOfFirst { it.attribute.id == goal.attributeId }
            if (attrIndex >= 0) rowBinding.spinnerAttr.setSelection(attrIndex)
            rowBinding.etTargetVal.setText(goal.targetValue.toString())
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "attr"
        }

        questWithDetails.behaviorGoals.forEach { goal ->
            val rowBinding = ItemQuestBehGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val behAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableBehaviors.map { it.behavior.name })
            behAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerBeh.adapter = behAdapter
            val behIndex = availableBehaviors.indexOfFirst { it.behavior.id == goal.behaviorId }
            if (behIndex >= 0) rowBinding.spinnerBeh.setSelection(behIndex)
            rowBinding.etTargetCount.setText(goal.targetCount.toString())
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "beh"
        }

        fun addEffectRow(container: ViewGroup, isPunishment: Boolean, existingEffect: QuestEffectEntity? = null) {
            val rowBinding = ItemQuestEffectBinding.inflate(layoutInflater, container, true)
            
            val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("属性变动", "文本描述"))
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerType.adapter = typeAdapter

            if (availableAttributes.isNotEmpty()) {
                val attrAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableAttributes.map { it.attribute.name })
                attrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                rowBinding.spinnerAttr.adapter = attrAdapter
            }

            existingEffect?.let { e ->
                rowBinding.spinnerType.setSelection(e.type)
                if (e.type == 0 && e.attributeId != null && e.valueChange != null) {
                    val attrIndex = availableAttributes.indexOfFirst { it.attribute.id == e.attributeId }
                    if (attrIndex >= 0) rowBinding.spinnerAttr.setSelection(attrIndex)
                    rowBinding.etValChange.setText(e.valueChange.toString())
                } else if (e.type == 1 && e.text != null) {
                    rowBinding.etTextDesc.setText(e.text)
                }
            }

            rowBinding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        rowBinding.llAttrChange.visibility = View.VISIBLE
                        rowBinding.tilText.visibility = View.GONE
                    } else {
                        rowBinding.llAttrChange.visibility = View.GONE
                        rowBinding.tilText.visibility = View.VISIBLE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            rowBinding.btnRemove.setOnClickListener { container.removeView(rowBinding.root) }
            rowBinding.root.tag = isPunishment
        }

        questWithDetails.effects.filter { !it.isPunishment }.forEach { e ->
            addEffectRow(dialogBinding.llRewardsContainer, false, e)
        }

        questWithDetails.effects.filter { it.isPunishment }.forEach { e ->
            addEffectRow(dialogBinding.llPunishmentsContainer, true, e)
        }

        dialogBinding.btnAddAttrGoal.setOnClickListener {
            if (availableAttributes.isEmpty()) {
                return@setOnClickListener
            }
            val rowBinding = ItemQuestAttrGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val attrAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableAttributes.map { it.attribute.name })
            attrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerAttr.adapter = attrAdapter
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "attr"
        }

        dialogBinding.btnAddBehGoal.setOnClickListener {
            if (availableBehaviors.isEmpty()) {
                return@setOnClickListener
            }
            val rowBinding = ItemQuestBehGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val behAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableBehaviors.map { it.behavior.name })
            behAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerBeh.adapter = behAdapter
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "beh"
        }

        dialogBinding.btnAddReward.setOnClickListener { addEffectRow(dialogBinding.llRewardsContainer, false) }
        dialogBinding.btnAddPunishment.setOnClickListener { addEffectRow(dialogBinding.llPunishmentsContainer, true) }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val type = when (dialogBinding.spinnerType.selectedItemPosition) {
                0 -> 0
                1 -> 3
                2 -> 1
                else -> 2
            }

            if (type != 0 && type != 3 && selectedDeadline == null) {
                return@setOnClickListener
            }

            val attrGoals = mutableListOf<QuestAttributeGoalEntity>()
            val behGoals = mutableListOf<QuestBehaviorGoalEntity>()

            for (i in 0 until dialogBinding.llGoalsContainer.childCount) {
                val view = dialogBinding.llGoalsContainer.getChildAt(i)
                if (view.tag == "attr") {
                    val rb = ItemQuestAttrGoalBinding.bind(view)
                    val pos = rb.spinnerAttr.selectedItemPosition
                    val target = rb.etTargetVal.text.toString().toFloatOrNull()
                    if (pos >= 0 && target != null && target > 0f) {
                        attrGoals.add(QuestAttributeGoalEntity(questId = quest.id, attributeId = availableAttributes[pos].attribute.id, targetValue = target))
                    }
                } else if (view.tag == "beh") {
                    val rb = ItemQuestBehGoalBinding.bind(view)
                    val pos = rb.spinnerBeh.selectedItemPosition
                    val target = rb.etTargetCount.text.toString().toIntOrNull()
                    if (pos >= 0 && target != null && target > 0) {
                        behGoals.add(QuestBehaviorGoalEntity(questId = quest.id, behaviorId = availableBehaviors[pos].behavior.id, targetCount = target, currentCount = questWithDetails.behaviorGoals.find { it.behaviorId == availableBehaviors[pos].behavior.id }?.currentCount ?: 0))
                    }
                }
            }

            if (attrGoals.isEmpty() && behGoals.isEmpty()) {
                return@setOnClickListener
            }

            val effects = mutableListOf<QuestEffectEntity>()
            
            fun parseEffectsContainer(container: ViewGroup) {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    val isPunish = view.tag as Boolean
                    val rb = ItemQuestEffectBinding.bind(view)
                    val effectType = rb.spinnerType.selectedItemPosition
                    
                    if (effectType == 0) {
                        val pos = rb.spinnerAttr.selectedItemPosition
                        val change = rb.etValChange.text.toString().toFloatOrNull()
                        if (pos >= 0 && change != null && availableAttributes.isNotEmpty()) {
                            effects.add(QuestEffectEntity(questId = quest.id, isPunishment = isPunish, type = 0, attributeId = availableAttributes[pos].attribute.id, valueChange = change))
                        }
                    } else {
                        val text = rb.etTextDesc.text?.toString()?.trim()
                        if (!text.isNullOrEmpty()) {
                            effects.add(QuestEffectEntity(questId = quest.id, isPunishment = isPunish, type = 1, text = text))
                        }
                    }
                }
            }
            
            parseEffectsContainer(dialogBinding.llRewardsContainer)
            parseEffectsContainer(dialogBinding.llPunishmentsContainer)

            val updatedQuest = quest.copy(
                name = name,
                type = type,
                deadline = selectedDeadline
            )
            
            viewModel.updateQuestWithDetails(updatedQuest, attrGoals, behGoals, effects)
            dialog.dismiss()
        }

        dialog.show()
    }
}
