package com.example.lifegame.ui.quest

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.databinding.ItemQuestBinding

class QuestAdapter(
    private val onQuestClick: (QuestWithDetails) -> Unit,
    private val onQuestLongClick: (QuestWithDetails) -> Unit,
    private val calculateProgress: (QuestWithDetails) -> Float
) : ListAdapter<QuestWithDetails, QuestAdapter.QuestViewHolder>(QuestDiffCallback()) {

    inner class QuestViewHolder(private val binding: ItemQuestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuestWithDetails) {
            val quest = item.quest
            binding.tvQuestName.text = quest.name
            
            // Status text and color
            when (quest.status) {
                0 -> {
                    binding.tvStatus.text = "进行中"
                    binding.tvStatus.setTextColor(Color.parseColor("#A0A0A0"))
                    binding.cardContainer.setCardBackgroundColor(Color.parseColor("#21212B"))
                }
                1 -> {
                    binding.tvStatus.text = "可领取"
                    binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                    binding.cardContainer.setCardBackgroundColor(Color.parseColor("#1B3320")) // subtle green tint
                }
                2 -> {
                    binding.tvStatus.text = "已领取"
                    binding.tvStatus.setTextColor(Color.parseColor("#606060"))
                    binding.cardContainer.setCardBackgroundColor(Color.parseColor("#1A1A1A"))
                }
                3 -> {
                    binding.tvStatus.text = "已失败"
                    binding.tvStatus.setTextColor(Color.parseColor("#F44336"))
                    binding.cardContainer.setCardBackgroundColor(Color.parseColor("#331A1A"))
                }
            }

            // Progress Bar
            val progress = calculateProgress(item)
            binding.pbProgress.progress = (progress * 100).toInt()

            // Deadline
            if (quest.type == 0) {
                binding.tvDeadline.text = "今日重置"
            } else if (quest.deadline != null) {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                binding.tvDeadline.text = "${format.format(quest.deadline)} 前"
            } else {
                binding.tvDeadline.text = "无期限"
            }

            binding.root.setOnClickListener {
                onQuestClick(item)
            }

            binding.root.setOnLongClickListener {
                onQuestLongClick(item)
                true
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