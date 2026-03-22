package com.example.lifegame.ui.attribute.rank

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.RankEntity
import com.example.lifegame.databinding.ItemRankBinding

class RankAdapter(
    private val onDeleteClick: (RankEntity) -> Unit
) : ListAdapter<RankEntity, RankAdapter.RankViewHolder>(RankDiffCallback()) {

    inner class RankViewHolder(private val binding: ItemRankBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rank: RankEntity) {
            binding.tvRankName.text = rank.name
            binding.tvRankRange.text = "${rank.minValue} - ${rank.maxValue}"
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(rank)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankViewHolder {
        val binding = ItemRankBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RankViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class RankDiffCallback : DiffUtil.ItemCallback<RankEntity>() {
    override fun areItemsTheSame(oldItem: RankEntity, newItem: RankEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RankEntity, newItem: RankEntity): Boolean {
        return oldItem == newItem
    }
}
