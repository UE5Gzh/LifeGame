package com.example.lifegame.ui.behavior

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.BehaviorGroupEntity
import com.example.lifegame.databinding.ItemBehaviorGroupBinding
import java.util.Collections

class BehaviorGroupAdapter(
    private val onEditClick: (BehaviorGroupEntity) -> Unit,
    private val onDeleteClick: (BehaviorGroupEntity) -> Unit
) : ListAdapter<BehaviorGroupEntity, BehaviorGroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList)
    }

    inner class GroupViewHolder(private val binding: ItemBehaviorGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: BehaviorGroupEntity) {
            binding.tvGroupName.text = group.name

            binding.ivEdit.setOnClickListener {
                onEditClick(group)
            }

            binding.ivDelete.setOnClickListener {
                onDeleteClick(group)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemBehaviorGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class GroupDiffCallback : DiffUtil.ItemCallback<BehaviorGroupEntity>() {
    override fun areItemsTheSame(oldItem: BehaviorGroupEntity, newItem: BehaviorGroupEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BehaviorGroupEntity, newItem: BehaviorGroupEntity): Boolean {
        return oldItem == newItem
    }
}