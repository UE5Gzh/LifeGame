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
    private val attributes: List<AttributeWithRanks>,
    private val onStatusToggle: (StatusEntity, Boolean) -> Unit,
    private val onEditClick: (StatusEntity) -> Unit,
    private val onDeleteClick: (StatusEntity) -> Unit
) : ListAdapter<StatusEntity, StatusAdapter.StatusViewHolder>(StatusDiffCallback()) {

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
                binding.tvEffectInfo.text = "周期性: 每${status.periodValue}$unitStr $attrName $sign${formatValue(status.changeValue)}"
                
                if (status.isEnabled) {
                    binding.tvNextTrigger.visibility = View.VISIBLE
                    val nextTrigger = calculateNextTriggerTime(status)
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    binding.tvNextTrigger.text = "下次触发: ${sdf.format(Date(nextTrigger))}"
                } else {
                    binding.tvNextTrigger.visibility = View.GONE
                }
            } else {
                binding.tvEffectInfo.text = "加成: $attrName +${formatValue(status.bonusPercent)}%"
                binding.tvNextTrigger.visibility = View.GONE
            }

            try {
                binding.cardContainer.setCardBackgroundColor(Color.parseColor(status.colorHex))
            } catch (e: Exception) {
                binding.cardContainer.setCardBackgroundColor(Color.parseColor("#21212B"))
            }

            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = status.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onStatusToggle(status, isChecked)
            }

            if (isSortMode) {
                binding.ivDragHandle.visibility = View.VISIBLE
                binding.switchEnabled.visibility = View.GONE
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.ivDragHandle.visibility = View.GONE
                binding.switchEnabled.visibility = View.VISIBLE
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE

                binding.btnEdit.setOnClickListener {
                    onEditClick(status)
                }

                binding.btnDelete.setOnClickListener {
                    onDeleteClick(status)
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
