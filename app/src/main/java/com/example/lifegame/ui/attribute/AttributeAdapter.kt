package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.databinding.ItemAttributeBinding
import java.util.Collections

class AttributeAdapter(
    private val onAttributeClick: (AttributeWithRanks) -> Unit,
    private val onAttributeLongClick: (AttributeWithRanks) -> Unit
) : ListAdapter<AttributeWithRanks, AttributeAdapter.AttributeViewHolder>(AttributeDiffCallback()) {

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

    inner class AttributeViewHolder(private val binding: ItemAttributeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttributeWithRanks) {
            val attribute = item.attribute
            binding.tvName.text = attribute.name
            binding.tvValue.text = attribute.currentValue.toString()
            binding.clContainer.setCardBackgroundColor(Color.parseColor(attribute.colorHex))
            
            // Handle rank display
            val ranks = item.ranks.sortedBy { it.minValue }
            val currentRank = ranks.lastOrNull { attribute.currentValue >= it.minValue }
            val nextRank = ranks.firstOrNull { attribute.currentValue < it.minValue }

            if (currentRank != null) {
                binding.tvRankName.visibility = View.VISIBLE
                binding.tvRankName.text = currentRank.name
            } else {
                binding.tvRankName.visibility = View.GONE
            }

            if (nextRank != null) {
                binding.tvProgress.visibility = View.VISIBLE
                val pointsNeeded = nextRank.minValue - attribute.currentValue
                binding.tvProgress.text = "距离「${nextRank.name}」还需 $pointsNeeded 点"
            } else {
                binding.tvProgress.visibility = View.GONE
            }

            if (isSortMode) {
                binding.ivDragHandle.visibility = View.VISIBLE
                binding.root.setOnClickListener(null)
                binding.root.setOnLongClickListener(null)
            } else {
                binding.ivDragHandle.visibility = View.GONE
                binding.root.setOnClickListener {
                    onAttributeClick(item)
                }
                binding.root.setOnLongClickListener {
                    onAttributeLongClick(item)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttributeViewHolder {
        val binding = ItemAttributeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AttributeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttributeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AttributeDiffCallback : DiffUtil.ItemCallback<AttributeWithRanks>() {
    override fun areItemsTheSame(oldItem: AttributeWithRanks, newItem: AttributeWithRanks): Boolean {
        return oldItem.attribute.id == newItem.attribute.id
    }

    override fun areContentsTheSame(oldItem: AttributeWithRanks, newItem: AttributeWithRanks): Boolean {
        return oldItem == newItem
    }
}
