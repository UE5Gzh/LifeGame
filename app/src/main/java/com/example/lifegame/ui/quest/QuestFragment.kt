package com.example.lifegame.ui.quest

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifegame.R
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.databinding.DialogCreateQuestBinding
import com.example.lifegame.databinding.FragmentQuestBinding
import com.example.lifegame.databinding.ItemQuestAttrGoalBinding
import com.example.lifegame.databinding.ItemQuestBehGoalBinding
import com.example.lifegame.databinding.ItemQuestEffectBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class QuestFragment : BaseFragment<FragmentQuestBinding>() {

    private val viewModel: QuestViewModel by viewModels()
    private lateinit var adapter: QuestAdapter
    private var currentSelectedTabType: Int = 0 // 0: Daily, 1: Main, 2: Side

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

        binding.btnAdd.setOnClickListener {
            showCreateQuestDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = QuestAdapter(
            onQuestClick = { quest ->
                handleQuestClick(quest)
            },
            onQuestLongClick = { quest ->
                showQuestOptionsDialog(quest)
            },
            calculateProgress = { quest ->
                viewModel.calculateProgress(quest, viewModel.attributes.value)
            }
        )
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.adapter = adapter
    }

    private fun setupTabLayout() {
        binding.tabQuestType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentSelectedTabType = tab?.position ?: 0
                filterQuests()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun filterQuests() {
        val allQuests = viewModel.quests.value
        val type = when (currentSelectedTabType) {
            0 -> 0 // 日常
            1 -> 1 // 主线
            2 -> 2 // 支线
            3 -> 3 // 周常
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
                        viewModel.checkQuestCompletions(it)
                        adapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.quests.collect {
                        viewModel.checkQuestCompletions(viewModel.attributes.value)
                        filterQuests()
                    }
                }
                launch {
                    viewModel.behaviors.collect {
                        // Just collect to keep the StateFlow active and fetch from DB
                    }
                }
            }
        }
    }

    private fun handleQuestClick(questWithDetails: QuestWithDetails) {
        val quest = questWithDetails.quest
        when (quest.status) {
            0 -> {
                // In Progress -> Show details (For now, let's show a simple info dialog)
                showQuestDetailsDialog(questWithDetails)
            }
            1 -> {
                // Completed Unclaimed -> Claim
                MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle("领取奖励")
                    .setMessage("恭喜完成任务「${quest.name}」！是否立即领取奖励？")
                    .setPositiveButton("领取") { _, _ ->
                        viewModel.claimReward(questWithDetails)
                        Toast.makeText(requireContext(), "奖励已领取！", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            2 -> {
                // Claimed
                Toast.makeText(requireContext(), "任务已完成并领取奖励", Toast.LENGTH_SHORT).show()
                showQuestDetailsDialog(questWithDetails)
            }
            3 -> {
                // Failed
                Toast.makeText(requireContext(), "任务已失败", Toast.LENGTH_SHORT).show()
                showQuestDetailsDialog(questWithDetails)
            }
        }
    }

    private fun showQuestOptionsDialog(questWithDetails: QuestWithDetails) {
        val options = mutableListOf("删除任务")
        if (questWithDetails.quest.status == 0) { // Only in progress quests can be given up
            options.add(0, "放弃任务") // Put it first
            if (questWithDetails.quest.isFocused) {
                options.add(1, "取消关注")
            } else {
                options.add(1, "设为关注任务")
            }
        }
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle(questWithDetails.quest.name)
            .setItems(options.toTypedArray()) { _, which ->
                val selected = options[which]
                if (selected == "删除任务") {
                    MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                        .setTitle("确认删除")
                        .setMessage("确定要彻底删除该任务吗？该操作不可恢复。")
                        .setPositiveButton("删除") { _, _ ->
                            viewModel.deleteQuest(questWithDetails.quest)
                            Toast.makeText(requireContext(), "任务已删除", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else if (selected == "放弃任务") {
                    MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                        .setTitle("确认放弃")
                        .setMessage("放弃任务将触发该任务的惩罚（如果有），确定放弃吗？")
                        .setPositiveButton("放弃") { _, _ ->
                            viewModel.giveUpQuest(questWithDetails)
                            Toast.makeText(requireContext(), "任务已放弃", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else if (selected == "设为关注任务" || selected == "取消关注") {
                    viewModel.toggleQuestFocus(questWithDetails.quest)
                }
            }
            .show()
    }

    private fun showQuestDetailsDialog(q: QuestWithDetails) {
        val msg = StringBuilder()
        msg.append("目标:\n")
        q.attributeGoals.forEach { ag ->
            val attrName = viewModel.attributes.value.find { it.attribute.id == ag.attributeId }?.attribute?.name ?: "未知属性"
            val currentVal = viewModel.attributes.value.find { it.attribute.id == ag.attributeId }?.attribute?.currentValue ?: 0
            val targetVal = ag.targetValue
            val progress = if (targetVal > 0) ((currentVal.toFloat() / targetVal) * 100).toInt().coerceAtMost(100) else 100
            msg.append("- $attrName $currentVal/$targetVal ($progress%)\n")
        }
        q.behaviorGoals.forEach { bg ->
            val behName = viewModel.behaviors.value.find { it.behavior.id == bg.behaviorId }?.behavior?.name ?: "未知行动"
            val targetCount = bg.targetCount
            val currentCount = bg.currentCount
            val progress = if (targetCount > 0) ((currentCount.toFloat() / targetCount) * 100).toInt().coerceAtMost(100) else 100
            msg.append("- $behName $currentCount/${targetCount}次 ($progress%)\n")
        }
        
        msg.append("\n奖励:\n")
        if (q.effects.none { !it.isPunishment }) msg.append("- 无\n")
        q.effects.filter { !it.isPunishment }.forEach { e ->
            if (e.type == 0) {
                val attrName = viewModel.attributes.value.find { it.attribute.id == e.attributeId }?.attribute?.name ?: "未知属性"
                val sign = if ((e.valueChange ?: 0) >= 0) "+" else ""
                msg.append("- $attrName $sign${e.valueChange}\n")
            } else {
                msg.append("- ${e.text}\n")
            }
        }

        val punishments = q.effects.filter { it.isPunishment }
        if (punishments.isNotEmpty()) {
            msg.append("\n惩罚:\n")
            punishments.forEach { e ->
                if (e.type == 0) {
                    val attrName = viewModel.attributes.value.find { it.attribute.id == e.attributeId }?.attribute?.name ?: "未知属性"
                    val sign = if ((e.valueChange ?: 0) >= 0) "+" else ""
                    msg.append("- $attrName $sign${e.valueChange}\n")
                } else {
                    msg.append("- ${e.text}\n")
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle(q.quest.name)
            .setMessage(msg.toString())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showCreateQuestDialog() {
        val dialogBinding = DialogCreateQuestBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)

        val availableAttributes = viewModel.attributes.value
        val availableBehaviors = viewModel.behaviors.value

        var selectedDeadline: Long? = null

        // Handle Type Radio Group
        dialogBinding.spinnerType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 0: 日常, 1: 主线, 2: 周常, 3: 支线
                if (position == 0 || position == 2) {
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

        // Handle Deadline Selection
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

        // Add Attribute Goal
        dialogBinding.btnAddAttrGoal.setOnClickListener {
            if (availableAttributes.isEmpty()) {
                Toast.makeText(requireContext(), "请先创建属性", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rowBinding = ItemQuestAttrGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableAttributes.map { it.attribute.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerAttr.adapter = adapter
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "attr"
        }

        // Add Behavior Goal
        dialogBinding.btnAddBehGoal.setOnClickListener {
            if (availableBehaviors.isEmpty()) {
                Toast.makeText(requireContext(), "请先创建行动", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rowBinding = ItemQuestBehGoalBinding.inflate(layoutInflater, dialogBinding.llGoalsContainer, true)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableBehaviors.map { it.behavior.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerBeh.adapter = adapter
            rowBinding.btnRemove.setOnClickListener { dialogBinding.llGoalsContainer.removeView(rowBinding.root) }
            rowBinding.root.tag = "beh"
        }

        // Helper to add Effect (Reward or Punishment)
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
                    if (position == 0) { // Attribute
                        rowBinding.llAttrChange.visibility = View.VISIBLE
                        rowBinding.tilText.visibility = View.GONE
                    } else { // Text
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
                Toast.makeText(requireContext(), "请输入任务名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = when (dialogBinding.spinnerType.selectedItemPosition) {
                0 -> 0 // 日常
                1 -> 1 // 主线
                2 -> 3 // 周常
                else -> 2 // 支线
            }

            if (type != 0 && type != 3 && selectedDeadline == null) {
                Toast.makeText(requireContext(), "请选择截止日期", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse Goals
            val attrGoals = mutableListOf<QuestAttributeGoalEntity>()
            val behGoals = mutableListOf<QuestBehaviorGoalEntity>()

            for (i in 0 until dialogBinding.llGoalsContainer.childCount) {
                val view = dialogBinding.llGoalsContainer.getChildAt(i)
                if (view.tag == "attr") {
                    val rb = ItemQuestAttrGoalBinding.bind(view)
                    val pos = rb.spinnerAttr.selectedItemPosition
                    val target = rb.etTargetVal.text.toString().toIntOrNull()
                    if (pos >= 0 && target != null && target > 0) {
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
                Toast.makeText(requireContext(), "请至少添加一个完成目标", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse Effects
            val effects = mutableListOf<QuestEffectEntity>()
            
            fun parseEffectsContainer(container: ViewGroup) {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    val isPunish = view.tag as Boolean
                    val rb = ItemQuestEffectBinding.bind(view)
                    val effectType = rb.spinnerType.selectedItemPosition // 0: attr, 1: text
                    
                    if (effectType == 0) {
                        val pos = rb.spinnerAttr.selectedItemPosition
                        val change = rb.etValChange.text.toString().toIntOrNull()
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
}