package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.databinding.ItemAttributeBinding

class AttributeAdapter : ListAdapter<AttributeEntity, AttributeAdapter.AttributeViewHolder>(AttributeDiffCallback()) {

    inner class AttributeViewHolder(private val binding: ItemAttributeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(attribute: AttributeEntity) {
            binding.tvName.text = attribute.name
            binding.tvValue.text = attribute.currentValue.toString()
            binding.clContainer.setCardBackgroundColor(Color.parseColor(attribute.colorHex))
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

class AttributeDiffCallback : DiffUtil.ItemCallback<AttributeEntity>() {
    override fun areItemsTheSame(oldItem: AttributeEntity, newItem: AttributeEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AttributeEntity, newItem: AttributeEntity): Boolean {
        return oldItem == newItem
    }
}
