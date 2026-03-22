package com.example.lifegame.ui.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.LogEntity
import com.example.lifegame.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<LogEntity, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: LogEntity, showDateHeader: Boolean) {
            val dateStr = dateFormat.format(Date(log.timestamp))
            val timeStr = timeFormat.format(Date(log.timestamp))

            if (showDateHeader) {
                binding.tvDateHeader.visibility = View.VISIBLE
                binding.tvDateHeader.text = dateStr
            } else {
                binding.tvDateHeader.visibility = View.GONE
            }

            binding.tvActionTitle.text = log.title
            binding.tvTime.text = timeStr
            
            if (log.details.isNotEmpty()) {
                binding.tvDetail.visibility = View.VISIBLE
                binding.tvDetail.text = log.details
            } else {
                binding.tvDetail.visibility = View.GONE
            }

            // Optional: color coding based on type
            when (log.type) {
                "BEHAVIOR_EXECUTION" -> binding.tvActionTitle.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                "QUEST_CREATION" -> binding.tvActionTitle.setTextColor(android.graphics.Color.parseColor("#2196F3"))
                "QUEST_COMPLETION" -> binding.tvActionTitle.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                "QUEST_ABANDON" -> binding.tvActionTitle.setTextColor(android.graphics.Color.parseColor("#F44336"))
                else -> binding.tvActionTitle.setTextColor(android.graphics.Color.parseColor("#BB86FC"))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val currentLog = getItem(position)
        var showDateHeader = true
        if (position > 0) {
            val previousLog = getItem(position - 1)
            val currentDateStr = dateFormat.format(Date(currentLog.timestamp))
            val previousDateStr = dateFormat.format(Date(previousLog.timestamp))
            if (currentDateStr == previousDateStr) {
                showDateHeader = false
            }
        }
        holder.bind(currentLog, showDateHeader)
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntity>() {
        override fun areItemsTheSame(oldItem: LogEntity, newItem: LogEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LogEntity, newItem: LogEntity): Boolean {
            return oldItem == newItem
        }
    }
}