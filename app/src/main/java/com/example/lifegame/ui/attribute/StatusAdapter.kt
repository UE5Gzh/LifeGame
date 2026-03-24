package com.example.lifegame.ui.attribute

import android.graphics.Color
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

            if (status.isEnabled && status.durationValue > 0) {
                binding.tvNextTrigger.visibility = View.VISIBLE
                binding.tvNextTrigger.text = calculateRemainingTime(status)
            } else {
                binding.tvNextTrigger.visibility = View.GONE
            }

            try {
                binding.cardContainer.setCardBackgroundColor(Color.parseColor(status.colorHex))
            } catch (e: Exception) {
                binding.cardContainer.setCardBackgroundColor(Color.parseColor("#21212B"))
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
                binding.switchEnabled.isChecked = status.isEnabled
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

        private fun buildEffectInfoText(effects: List<StatusEffectEntity>): String {
            if (effects.isEmpty()) return "无效果"
            
            return effects.mapIndexed { index, effect ->
                val attrName = attributes.find { it.attribute.id == effect.targetAttributeId }?.attribute?.name ?: "未知属性"
                val prefix = if (effects.size > 1) "${index + 1}. " else ""
                
                when (effect.effectType) {
                    0 -> {
                        val unitStr = if (effect.periodUnit == 0) "小时" else "天"
                        val sign = if (effect.changeValue >= 0) "+" else ""
                        "$prefix$attrName $sign${formatValue(effect.changeValue)}/$unitStr"
                    }
                    1 -> "${prefix}获取$attrName +${formatValue(effect.bonusPercent)}%"
                    else -> "${prefix}获取$attrName -${formatValue(effect.bonusPercent)}%"
                }
            }.joinToString("\n")
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
