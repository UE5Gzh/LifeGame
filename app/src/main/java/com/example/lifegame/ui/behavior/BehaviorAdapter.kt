package com.example.lifegame.ui.behavior

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
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.example.lifegame.databinding.ItemBehaviorBinding
import java.util.Collections

class BehaviorAdapter(
    private val onActionClick: (BehaviorWithModifiers) -> Unit,
    private val onItemClick: (BehaviorWithModifiers) -> Unit,
    private val onItemLongClick: (BehaviorWithModifiers) -> Unit
) : ListAdapter<BehaviorWithModifiers, BehaviorAdapter.BehaviorViewHolder>(BehaviorDiffCallback()) {

    private var attributes: List<AttributeWithRanks> = emptyList()

    var isSortMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun setAttributes(newAttributes: List<AttributeWithRanks>) {
        attributes = newAttributes
        notifyDataSetChanged()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList)
    }

    inner class BehaviorViewHolder(private val binding: ItemBehaviorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BehaviorWithModifiers) {
            val behavior = item.behavior
            binding.tvName.text = behavior.name

            // Construct the effects text
            val builder = SpannableStringBuilder()
            
            // Energy part
            val energyText = if (behavior.energyType == 0) "⚡ -${behavior.energyValue}" else "⚡ +${behavior.energyValue}"
            builder.append(energyText)
            val energyColor = if (behavior.energyType == 0) Color.parseColor("#FFD54F") else Color.parseColor("#81C784")
            builder.setSpan(
                ForegroundColorSpan(energyColor),
                0,
                energyText.length,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Modifiers part
            for (modifier in item.modifiers) {
                val attribute = attributes.find { it.attribute.id == modifier.attributeId }?.attribute
                if (attribute != null) {
                    builder.append("  ")
                    val start = builder.length
                    val sign = if (modifier.valueChange > 0) "+" else ""
                    val modText = "${attribute.name}${sign}${modifier.valueChange}"
                    builder.append(modText)
                    
                    val attrColor = try {
                        Color.parseColor(attribute.colorHex)
                    } catch (e: Exception) {
                        Color.WHITE
                    }
                    builder.setSpan(
                        ForegroundColorSpan(attrColor),
                        start,
                        builder.length,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            binding.tvEffects.text = builder

            if (isSortMode) {
                binding.ivDragHandle.visibility = View.VISIBLE
                binding.btnAction.visibility = View.GONE
                binding.root.setOnClickListener(null)
                binding.root.setOnLongClickListener(null)
            } else {
                binding.ivDragHandle.visibility = View.GONE
                binding.btnAction.visibility = View.VISIBLE
                // Action button
                if (behavior.focusDuration == 0) {
                    binding.btnAction.text = "执行"
                } else {
                    binding.btnAction.text = "专注 ${behavior.focusDuration}m"
                }

                binding.btnAction.setOnClickListener {
                    onActionClick(item)
                }

                binding.root.setOnClickListener {
                    onItemClick(item)
                }

                binding.root.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BehaviorViewHolder {
        val binding = ItemBehaviorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BehaviorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BehaviorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class BehaviorDiffCallback : DiffUtil.ItemCallback<BehaviorWithModifiers>() {
    override fun areItemsTheSame(oldItem: BehaviorWithModifiers, newItem: BehaviorWithModifiers): Boolean {
        return oldItem.behavior.id == newItem.behavior.id
    }

    override fun areContentsTheSame(oldItem: BehaviorWithModifiers, newItem: BehaviorWithModifiers): Boolean {
        return oldItem == newItem
    }
}