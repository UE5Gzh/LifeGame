package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.data.entity.StatusWithEffects
import com.example.lifegame.databinding.ItemStatusBinding
import java.util.Collections
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
            
            if (status.description.isNotEmpty()) {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = status.description
            } else {
                binding.tvDescription.visibility = View.GONE
            }

            val effectInfoText = buildEffectInfoText(effects)
            binding.tvEffectInfo.text = effectInfoText

            val isExpired = status.durationValue > 0 && isStatusExpired(status)
            
            if (status.durationValue > 0) {
                binding.tvDuration.visibility = View.VISIBLE
                if (isExpired) {
                    binding.tvDuration.text = "⏱ 已到期"
                    binding.tvDuration.setTextColor(Color.parseColor("#F44336"))
                } else {
                    binding.tvDuration.text = "⏱ ${calculateRemainingTime(status)}"
                    binding.tvDuration.setTextColor(Color.parseColor("#888888"))
                }
            } else {
                binding.tvDuration.visibility = View.GONE
            }

            try {
                binding.viewColorIndicator.setBackgroundColor(Color.parseColor(status.colorHex))
            } catch (e: Exception) {
                binding.viewColorIndicator.setBackgroundColor(Color.parseColor("#9C27B0"))
            }

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

        private fun buildEffectInfoText(effects: List<StatusEffectEntity>): SpannableStringBuilder {
            val builder = SpannableStringBuilder()
            if (effects.isEmpty()) {
                builder.append("无效果")
                return builder
            }
            
            effects.forEachIndexed { index, effect ->
                val attr = attributes.find { it.attribute.id == effect.targetAttributeId }?.attribute
                val attrName = attr?.name ?: "未知属性"
                val attrColor = try {
                    Color.parseColor(attr?.colorHex ?: "#FFFFFF")
                } catch (e: Exception) {
                    Color.WHITE
                }
                
                if (index > 0) builder.append("\n")
                
                when (effect.effectType) {
                    0 -> {
                        val unitStr = when (effect.periodUnit) {
                            0 -> "分钟"
                            1 -> "小时"
                            else -> "天"
                        }
                        val sign = if (effect.changeValue >= 0) "+" else ""
                        val valueStr = formatValue(effect.changeValue)
                        
                        val start = builder.length
                        builder.append(attrName)
                        builder.setSpan(
                            ForegroundColorSpan(attrColor),
                            start,
                            builder.length,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.append(" $sign$valueStr/$unitStr")
                    }
                    1 -> {
                        val start = builder.length
                        builder.append(attrName)
                        builder.setSpan(
                            ForegroundColorSpan(attrColor),
                            start,
                            builder.length,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.append(" +${formatValue(effect.bonusPercent)}%")
                    }
                    else -> {
                        val start = builder.length
                        builder.append(attrName)
                        builder.setSpan(
                            ForegroundColorSpan(attrColor),
                            start,
                            builder.length,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.append(" -${formatValue(effect.bonusPercent)}%")
                    }
                }
            }
            
            return builder
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
                days > 0 -> "剩余${days}天"
                hours > 0 -> "剩余${hours}小时"
                minutes > 0 -> "剩余${minutes}分钟"
                else -> "即将到期"
            }
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
