package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.data.entity.StatusWithEffects
import com.example.lifegame.databinding.ItemEffectTagBinding
import com.example.lifegame.databinding.ItemStatusBinding
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.roundToInt

class StatusAdapter(
    private var attributes: List<AttributeWithRanks>,
    private val onStatusToggle: (StatusEntity, Boolean) -> Unit,
    private val onItemClick: (StatusEntity) -> Unit,
    private val onItemLongClick: (StatusEntity) -> Unit
) : ListAdapter<StatusWithEffects, StatusAdapter.StatusViewHolder>(StatusDiffCallback()) {

    var isSortMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun updateAttributes(newAttributes: List<AttributeWithRanks>) {
        attributes = newAttributes
        notifyDataSetChanged()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList)
    }

    fun getStatusesInOrder(): List<StatusEntity> {
        return currentList.map { it.status }
    }

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    inner class StatusViewHolder(private val binding: ItemStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(statusWithEffects: StatusWithEffects) {
            val status = statusWithEffects.status
            val effects = statusWithEffects.effects

            binding.tvName.text = status.name

            // 设置颜色指示器
            try {
                binding.viewColorIndicator.background.setTint(Color.parseColor(status.colorHex))
            } catch (e: Exception) {
                binding.viewColorIndicator.background.setTint(Color.parseColor("#9C27B0"))
            }

            // 构建效果标签
            buildEffectTags(effects, status)

            // 处理持续时间
            val isExpired = status.durationValue > 0 && isStatusExpired(status)
            if (status.durationValue > 0) {
                binding.llDurationContainer.visibility = View.VISIBLE
                if (isExpired) {
                    binding.tvDuration.text = "⏱ 已到期"
                    binding.tvDuration.setTextColor(ContextCompat.getColor(binding.root.context, R.color.attribute_negative))
                    binding.pbDuration.progress = 0
                } else {
                    val remaining = calculateRemainingTime(status)
                    binding.tvDuration.text = "⏱ 剩余 $remaining"
                    binding.tvDuration.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_hint))
                    val progress = calculateDurationProgress(status)
                    binding.pbDuration.progress = progress
                }
            } else {
                binding.llDurationContainer.visibility = View.GONE
            }

            // 排序模式处理
            if (isSortMode) {
                binding.ivDragHandle.visibility = View.VISIBLE
                binding.switchEnabled.visibility = View.GONE
                binding.contentArea.setOnClickListener(null)
                binding.contentArea.setOnLongClickListener(null)
            } else {
                binding.ivDragHandle.visibility = View.GONE
                binding.switchEnabled.visibility = View.VISIBLE

                binding.switchEnabled.setOnCheckedChangeListener(null)
                binding.switchEnabled.isChecked = status.isEnabled && !isExpired
                binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    onStatusToggle(status, isChecked)
                }

                binding.contentArea.setOnClickListener {
                    onItemClick(status)
                }

                binding.contentArea.setOnLongClickListener {
                    onItemLongClick(status)
                    true
                }
            }
        }

        private fun buildEffectTags(effects: List<StatusEffectEntity>, status: StatusEntity) {
            binding.llEffectTags.removeAllViews()

            if (effects.isEmpty()) {
                val emptyTag = createEmptyTag()
                binding.llEffectTags.addView(emptyTag)
                return
            }

            effects.forEach { effect ->
                val tagView = createEffectTag(effect, status)
                binding.llEffectTags.addView(tagView)
            }
        }

        private fun createEmptyTag(): View {
            val binding = ItemEffectTagBinding.inflate(
                LayoutInflater.from(binding.root.context),
                binding.llEffectTags,
                false
            )
            binding.tvEffectType.text = "无效果"
            binding.tvAttrName.text = "—"
            binding.tvEffectValue.text = ""
            binding.tvEffectValue.visibility = View.GONE
            binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.bg_effect_tag)
            return binding.root
        }

        private fun createEffectTag(effect: StatusEffectEntity, status: StatusEntity): View {
            val binding = ItemEffectTagBinding.inflate(
                LayoutInflater.from(binding.root.context),
                binding.llEffectTags,
                false
            )

            val attr = attributes.find { it.attribute.id == effect.targetAttributeId }?.attribute
            val attrName = attr?.name ?: "未知"

            when (effect.effectType) {
                0 -> {
                    // 周期性效果
                    binding.tvEffectType.text = "⏱ 周期"
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.bg_effect_tag)

                    val unitStr = when (effect.periodUnit) {
                        0 -> "分钟"
                        1 -> "小时"
                        else -> "天"
                    }
                    val sign = if (effect.changeValue >= 0) "+" else ""
                    binding.tvAttrName.text = attrName
                    binding.tvEffectValue.text = "$sign${formatValue(effect.changeValue)}/$unitStr"
                    binding.tvEffectValue.setTextColor(
                        if (effect.changeValue >= 0)
                            ContextCompat.getColor(binding.root.context, R.color.attribute_positive)
                        else
                            ContextCompat.getColor(binding.root.context, R.color.attribute_negative)
                    )

                    // 显示下次触发时间
                    binding.tvNextTrigger.visibility = View.VISIBLE
                    binding.tvNextTrigger.text = "下次: ${calculateNextTriggerTime(effect, status)}"
                }
                1 -> {
                    // 加成效果
                    binding.tvEffectType.text = "⭐ 加成"
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.bg_effect_tag_positive)
                    binding.tvAttrName.text = attrName
                    binding.tvEffectValue.text = "+${formatValue(effect.bonusPercent)}%"
                    binding.tvEffectValue.setTextColor(ContextCompat.getColor(binding.root.context, R.color.attribute_positive))
                }
                else -> {
                    // 衰减效果
                    binding.tvEffectType.text = "📉 衰减"
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.bg_effect_tag_negative)
                    binding.tvAttrName.text = attrName
                    binding.tvEffectValue.text = "-${formatValue(effect.bonusPercent)}%"
                    binding.tvEffectValue.setTextColor(ContextCompat.getColor(binding.root.context, R.color.attribute_negative))
                }
            }

            return binding.root
        }

        private fun calculateNextTriggerTime(effect: StatusEffectEntity, status: StatusEntity): String {
            val periodMillis = when (effect.periodUnit) {
                0 -> effect.periodValue * 60 * 1000L
                1 -> effect.periodValue * 60 * 60 * 1000L
                else -> effect.periodValue * 24 * 60 * 60 * 1000L
            }

            val elapsed = System.currentTimeMillis() - status.startTime
            val periodsElapsed = ceil(elapsed.toDouble() / periodMillis).toLong()
            val nextTrigger = status.startTime + periodMillis * periodsElapsed

            val javaDate = java.util.Date(nextTrigger)
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return format.format(javaDate)
        }

        private fun calculateRemainingTime(status: StatusEntity): String {
            val durationMillis = when (status.durationUnit) {
                0 -> status.durationValue * 60 * 1000L
                1 -> status.durationValue * 60 * 60 * 1000L
                else -> status.durationValue * 24 * 60 * 60 * 1000L
            }

            val elapsed = System.currentTimeMillis() - status.startTime
            val remaining = durationMillis - elapsed

            if (remaining <= 0) {
                return "已到期"
            }

            val minutes = remaining / (60 * 1000)
            val hours = minutes / 60
            val days = hours / 24

            return when {
                days > 0 -> "${days}天${hours % 24}小时"
                hours > 0 -> "${hours}小时${minutes % 60}分钟"
                minutes > 0 -> "${minutes}分钟"
                else -> "即将到期"
            }
        }

        private fun calculateDurationProgress(status: StatusEntity): Int {
            val durationMillis = when (status.durationUnit) {
                0 -> status.durationValue * 60 * 1000L
                1 -> status.durationValue * 60 * 60 * 1000L
                else -> status.durationValue * 24 * 60 * 60 * 1000L
            }

            val elapsed = System.currentTimeMillis() - status.startTime
            val progress = ((durationMillis - elapsed).toFloat() / durationMillis * 100).coerceIn(0f, 100f)
            return progress.roundToInt()
        }

        private fun isStatusExpired(status: StatusEntity): Boolean {
            if (status.durationValue <= 0) return false

            val durationMillis = when (status.durationUnit) {
                0 -> status.durationValue * 60 * 1000L
                1 -> status.durationValue * 60 * 60 * 1000L
                else -> status.durationValue * 24 * 60 * 60 * 1000L
            }

            val elapsed = System.currentTimeMillis() - status.startTime
            return elapsed >= durationMillis
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val binding = ItemStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class StatusDiffCallback : DiffUtil.ItemCallback<StatusWithEffects>() {
    override fun areItemsTheSame(oldItem: StatusWithEffects, newItem: StatusWithEffects): Boolean {
        return oldItem.status.id == newItem.status.id
    }

    override fun areContentsTheSame(oldItem: StatusWithEffects, newItem: StatusWithEffects): Boolean {
        return oldItem == newItem
    }
}
