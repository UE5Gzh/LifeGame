package com.example.lifegame.ui.quest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.databinding.ItemQuestBinding
import java.util.Calendar
import java.util.Collections

class QuestAdapter(
    private val onQuestClick: (QuestWithDetails) -> Unit,
    private val onQuestLongClick: (QuestWithDetails) -> Unit,
    private val calculateProgress: (QuestWithDetails) -> Float
) : ListAdapter<QuestWithDetails, QuestAdapter.QuestViewHolder>(QuestDiffCallback()) {

    var isSortMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList)
    }

    inner class QuestViewHolder(private val binding: ItemQuestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuestWithDetails) {
            val quest = item.quest
            binding.tvQuestName.text = quest.name
            
            val typeText = when (quest.type) {
                0 -> "日常"
                1 -> "主线"
                2 -> "支线"
                3 -> "周常"
                else -> "任务"
            }
            binding.tvQuestType.text = typeText
            
            if (isSortMode) {
                binding.ivDragHandle.visibility = View.VISIBLE
                binding.ivFocusedStar.visibility = View.GONE
                binding.tvQuestType.visibility = View.GONE
                binding.root.setOnClickListener(null)
                binding.root.setOnLongClickListener(null)
            } else {
                binding.ivDragHandle.visibility = View.GONE
                
                if (quest.isFocused) {
                    binding.ivFocusedStar.visibility = View.VISIBLE
                    binding.cardContainer.strokeWidth = 2
                    binding.cardContainer.strokeColor = Color.parseColor("#FFC107")
                } else {
                    binding.ivFocusedStar.visibility = View.GONE
                    binding.cardContainer.strokeWidth = 0
                }

                when (quest.status) {
                    0 -> {
                        binding.tvStatus.text = "进行中"
                        binding.tvStatus.setTextColor(Color.parseColor("#A0A0A0"))
                        binding.cardContainer.setCardBackgroundColor(Color.parseColor("#21212B"))
                        binding.pbProgress.setIndicatorColor(Color.parseColor("#9C27B0"))
                    }
                    1 -> {
                        binding.tvStatus.text = "可领取"
                        binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                        binding.cardContainer.setCardBackgroundColor(Color.parseColor("#1B3320"))
                        binding.pbProgress.setIndicatorColor(Color.parseColor("#4CAF50"))
                    }
                    2 -> {
                        binding.tvStatus.text = "已领取"
                        binding.tvStatus.setTextColor(Color.parseColor("#606060"))
                        binding.cardContainer.setCardBackgroundColor(Color.parseColor("#1A1A1A"))
                        binding.pbProgress.setIndicatorColor(Color.parseColor("#606060"))
                    }
                    3 -> {
                        binding.tvStatus.text = "已失败"
                        binding.tvStatus.setTextColor(Color.parseColor("#F44336"))
                        binding.cardContainer.setCardBackgroundColor(Color.parseColor("#331A1A"))
                        binding.pbProgress.setIndicatorColor(Color.parseColor("#F44336"))
                    }
                }

                val progress = calculateProgress(item)
                binding.pbProgress.progress = (progress * 100).toInt()
                binding.tvProgressPercent.text = "${(progress * 100).toInt()}%"

                binding.tvDeadline.text = formatDeadlineText(quest.type, quest.deadline, quest.status, quest.lastResetTime)

                binding.root.setOnClickListener {
                    onQuestClick(item)
                }

                binding.root.setOnLongClickListener {
                    onQuestLongClick(item)
                    true
                }
            }
        }

        private fun formatDeadlineText(type: Int, deadline: Long?, status: Int, lastResetTime: Long): String {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val nextDayStart = todayStart + 24 * 60 * 60 * 1000L
            
            val weekStart = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis > System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, -1)
                }
            }.timeInMillis
            
            val nextWeekStart = weekStart + 7 * 24 * 60 * 60 * 1000L
            
            val isCompleted = status == 1 || status == 2
            val isFailed = status == 3

            when (type) {
                0 -> {
                    val resetTime = nextDayStart
                    val remainingMs = resetTime - now
                    val remainingHours = (remainingMs / (1000 * 60 * 60)).toInt()
                    
                    return when {
                        isFailed -> "已过期"
                        isCompleted && remainingHours > 0 -> "已完成，还剩${remainingHours}小时重置"
                        isCompleted -> "已完成"
                        remainingHours > 0 -> "还剩${remainingHours}小时重置"
                        else -> "即将重置"
                    }
                }
                3 -> {
                    val resetTime = nextWeekStart
                    val remainingMs = resetTime - now
                    val remainingDays = (remainingMs / (1000 * 60 * 60 * 24)).toInt()
                    
                    return when {
                        isFailed -> "已过期"
                        isCompleted && remainingDays > 0 -> "已完成，还剩${remainingDays}天重置"
                        isCompleted -> "已完成"
                        remainingDays > 0 -> "还剩${remainingDays}天重置"
                        else -> "即将重置"
                    }
                }
                1, 2 -> {
                    if (deadline == null) {
                        return if (isCompleted) "已完成" else "无期限"
                    }
                    
                    val deadlineStart = Calendar.getInstance().apply {
                        timeInMillis = deadline
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    
                    val remainingMs = deadlineStart - now
                    
                    return when {
                        isFailed -> "已过期"
                        isCompleted -> "已完成"
                        remainingMs < 0 -> "已过期"
                        else -> {
                            val remainingDays = (remainingMs / (1000 * 60 * 60 * 24)).toInt()
                            if (remainingDays > 0) {
                                "还剩${remainingDays}天截止"
                            } else {
                                val remainingHours = (remainingMs / (1000 * 60 * 60)).toInt()
                                if (remainingHours > 0) {
                                    "还剩${remainingHours}小时截止"
                                } else {
                                    "即将截止"
                                }
                            }
                        }
                    }
                }
                else -> {
                    return if (isCompleted) "已完成" else if (isFailed) "已过期" else "无期限"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val binding = ItemQuestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class QuestDiffCallback : DiffUtil.ItemCallback<QuestWithDetails>() {
    override fun areItemsTheSame(oldItem: QuestWithDetails, newItem: QuestWithDetails): Boolean {
        return oldItem.quest.id == newItem.quest.id
    }

    override fun areContentsTheSame(oldItem: QuestWithDetails, newItem: QuestWithDetails): Boolean {
        return oldItem == newItem
    }
}
