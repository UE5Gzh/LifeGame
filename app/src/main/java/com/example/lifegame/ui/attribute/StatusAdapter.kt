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
import com.example.lifegame.databinding.ItemStatusBinding
import java.util.Collections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class StatusAdapter(
    private var attributes: List<AttributeWithRanks>,
    private val onStatusToggle: (StatusEntity, Boolean) -> Unit,
    private val onItemClick: (StatusEntity) -> Unit,
    private val onItemLongClick: (StatusEntity) -> Unit
) : ListAdapter<StatusEntity, StatusAdapter.StatusViewHolder>(StatusDiffCallback()) {

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

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    inner class StatusViewHolder(private val binding: ItemStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(status: StatusEntity) {
            binding.tvName.text = status.name
            
            if (status.description.isNotEmpty()) {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = status.description
            } else {
                binding.tvDescription.visibility = View.GONE
            }

            val attrName = attributes.find { it.attribute.id == status.targetAttributeId }?.attribute?.name ?: "未知属性"
            
            if (status.effectType == 0) {
                val unitStr = if (status.periodUnit == 0) "小时" else "天"
                val sign = if (status.changeValue >= 0) "+" else ""
                binding.tvEffectInfo.text = "效果: $attrName $sign${formatValue(status.changeValue)}/$unitStr"
                
                if (status.isEnabled) {
                    binding.tvNextTrigger.visibility = View.VISIBLE
                    val nextTrigger = calculateNextTriggerTime(status)
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    val nextTriggerText = "下次触发: ${sdf.format(Date(nextTrigger))}"
                    
                    if (status.durationValue > 0) {
                        val remainingText = calculateRemainingTime(status)
                        binding.tvNextTrigger.text = "$nextTriggerText | $remainingText"
                    } else {
                        binding.tvNextTrigger.text = nextTriggerText
                    }
                } else {
                    binding.tvNextTrigger.visibility = View.GONE
                }
            } else if (status.effectType == 1) {
                binding.tvEffectInfo.text = "加成: 获取$attrName +${formatValue(status.bonusPercent)}%"
                if (status.isEnabled && status.durationValue > 0) {
                    binding.tvNextTrigger.visibility = View.VISIBLE
                    binding.tvNextTrigger.text = calculateRemainingTime(status)
                } else {
                    binding.tvNextTrigger.visibility = View.GONE
                }
            } else {
                binding.tvEffectInfo.text = "衰减: 获取$attrName -${formatValue(status.bonusPercent)}%"
                if (status.isEnabled && status.durationValue > 0) {
                    binding.tvNextTrigger.visibility = View.VISIBLE
                    binding.tvNextTrigger.text = calculateRemainingTime(status)
                } else {
                    binding.tvNextTrigger.visibility = View.GONE
                }
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

        private fun calculateNextTriggerTime(status: StatusEntity): Long {
            val periodMillis = if (status.periodUnit == 0) {
                status.periodValue * 60 * 60 * 1000L
            } else {
                status.periodValue * 24 * 60 * 60 * 1000L
            }
            
            val lastTrigger = if (status.lastTriggerTime == 0L) status.startTime else status.lastTriggerTime
            var nextTrigger = lastTrigger + periodMillis
            
            val now = System.currentTimeMillis()
            while (nextTrigger <= now) {
                nextTrigger += periodMillis
            }
            
            return nextTrigger
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

class StatusDiffCallback : DiffUtil.ItemCallback<StatusEntity>() {
    override fun areItemsTheSame(oldItem: StatusEntity, newItem: StatusEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: StatusEntity, newItem: StatusEntity): Boolean {
        return oldItem == newItem
    }
}
