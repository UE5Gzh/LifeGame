package com.example.lifegame.ui.log

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R
import com.example.lifegame.data.entity.LogEntity
import com.example.lifegame.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter(
    private val onLogLongClick: (LogEntity) -> Unit
) : ListAdapter<LogEntity, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 日志类型对应的颜色和图标
    private data class LogTypeStyle(val color: Int, val icon: String)
    private val typeStyles = mapOf(
        "BEHAVIOR_EXECUTION" to LogTypeStyle(Color.parseColor("#4CAF50"), "⚡"),
        "BEHAVIOR_CREATION" to LogTypeStyle(Color.parseColor("#8BC34A"), "➕"),
        "QUEST_CREATION" to LogTypeStyle(Color.parseColor("#2196F3"), "📋"),
        "QUEST_COMPLETION" to LogTypeStyle(Color.parseColor("#FFC107"), "🎉"),
        "QUEST_ABANDON" to LogTypeStyle(Color.parseColor("#F44336"), "❌"),
        "QUEST_EDIT" to LogTypeStyle(Color.parseColor("#03A9F4"), "✏️"),
        "RANK_UP" to LogTypeStyle(Color.parseColor("#9C27B0"), "⬆️"),
        "STATUS_ADD" to LogTypeStyle(Color.parseColor("#00BCD4"), "🏷️"),
        "STATUS_REMOVE" to LogTypeStyle(Color.parseColor("#795548"), "🚫"),
        "STATUS_TRIGGER" to LogTypeStyle(Color.parseColor("#009688"), "⏰"),
        "ENERGY_RESET" to LogTypeStyle(Color.parseColor("#FF9800"), "🔄"),
        "ATTRIBUTE_CHANGE" to LogTypeStyle(Color.parseColor("#E91E63"), "📊"),
        "FOCUS_START" to LogTypeStyle(Color.parseColor("#673AB7"), "🎯"),
        "FOCUS_END" to LogTypeStyle(Color.parseColor("#3F51B5"), "⏹️"),
        "MANUAL_LOG" to LogTypeStyle(Color.parseColor("#607D8B"), "📝")
    )

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

            // 获取类型样式
            val style = typeStyles[log.type] ?: LogTypeStyle(Color.parseColor("#9C27B0"), "📋")

            // 设置类型颜色指示条
            binding.viewTypeIndicator.setBackgroundColor(style.color)

            // 设置类型图标
            binding.tvTypeIcon.text = style.icon

            // 设置标题颜色
            binding.tvActionTitle.setTextColor(style.color)

            // 锁定状态处理
            if (log.isLocked) {
                binding.ivLock.visibility = View.VISIBLE
                binding.cardLog.strokeWidth = 2
                binding.cardLog.strokeColor = ContextCompat.getColor(binding.root.context, R.color.purple_500)
                binding.cardLog.setCardBackgroundColor(Color.parseColor("#1A1A30"))
            } else {
                binding.ivLock.visibility = View.GONE
                binding.cardLog.strokeWidth = 0
                binding.cardLog.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.card_dark))
            }

            binding.root.setOnLongClickListener {
                onLogLongClick(log)
                true
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
