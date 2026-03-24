package com.example.lifegame.ui.log

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.example.lifegame.data.entity.LogEntity
import com.example.lifegame.databinding.FragmentLogBinding
import com.example.lifegame.ui.base.BaseFragment
import com.example.lifegame.util.LogExportHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class LogFragment : BaseFragment<FragmentLogBinding>() {

    private val viewModel: LogViewModel by viewModels()
    private lateinit var logAdapter: LogAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLogBinding {
        return FragmentLogBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        
        logAdapter = LogAdapter(
            onLogLongClick = { log ->
                showLogOptionsDialog(log)
            }
        )
        binding.rvLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
        }

        binding.btnAdd.setOnClickListener {
            showAddLogDialog()
        }

        binding.btnClear.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle("清空日志")
                .setMessage("确定要清空所有未锁定的日志记录吗？")
                .setPositiveButton("确定") { _, _ ->
                    viewModel.clearLogs()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        binding.btnSearch.setOnClickListener {
            showDatePickerForSearch()
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showLogOptionsDialog(log: LogEntity) {
        val lockText = if (log.isLocked) "解锁此日志" else "锁定此日志"
        val options = arrayOf("删除此日志", lockText)

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("日志操作")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.deleteLog(log)
                    1 -> viewModel.toggleLogLock(log)
                }
            }
            .show()
    }

    private fun showAddLogDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "输入你的想法..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 10
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
        }

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("写日志")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.insertCustomLog(content)
                    Toast.makeText(requireContext(), "日志已保存并锁定", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "内容不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDatePickerForSearch() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                scrollToDate(selectedCalendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun scrollToDate(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDateStr = dateFormat.format(date)
        val logs = logAdapter.currentList
        
        val targetPosition = logs.indexOfFirst { log ->
            dateFormat.format(Date(log.timestamp)) == targetDateStr
        }

        if (targetPosition != -1) {
            val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
            smoothScroller.targetPosition = targetPosition
            binding.rvLogs.layoutManager?.startSmoothScroll(smoothScroller)
        } else {
            Toast.makeText(requireContext(), "该日期无日志记录", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettingsDialog() {
        val settingsView = LayoutInflater.from(requireContext()).inflate(
            android.R.layout.select_dialog_item, null, false
        )
        
        val options = arrayOf("导出日志", "日志存储设置", "任务日志默认锁定设置")
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("日志设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportLogs()
                    1 -> showStorageSettingsDialog()
                    2 -> showDefaultLockSettingsDialog()
                }
            }
            .show()
    }

    private fun exportLogs() {
        val logs = logAdapter.currentList
        if (logs.isEmpty()) {
            Toast.makeText(requireContext(), "没有日志可导出", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val file = LogExportHelper.exportLogsToCsv(requireContext(), logs)
            if (file != null) {
                MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle("导出成功")
                    .setMessage("日志已导出到:\n${file.name}\n\n是否立即分享?")
                    .setPositiveButton("分享") { _, _ ->
                        LogExportHelper.shareFile(requireContext(), file)
                    }
                    .setNegativeButton("稍后", null)
                    .show()
            }
        }
    }

    private fun showStorageSettingsDialog() {
        val currentLimit = viewModel.getMaxLogLimit()
        
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentLimit.toString())
            setSelection(text.length)
        }

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("日志存储设置")
            .setMessage("设置最大日志存储数量 (50 - 20000)\n保存后将自动清理超出限制的未锁定日志。")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val inputStr = editText.text.toString()
                val limit = inputStr.toIntOrNull()
                
                if (limit != null && limit in 50..20000) {
                    viewModel.setMaxLogLimit(limit)
                    Toast.makeText(requireContext(), "已更新最大日志数量为 $limit", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "请输入50到20000之间的数字", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDefaultLockSettingsDialog() {
        val settings = viewModel.getDefaultLockSettings()
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.example.lifegame.R.layout.dialog_default_lock_settings, null, false
        )
        
        val cbDaily = dialogView.findViewById<CheckBox>(com.example.lifegame.R.id.cb_daily_quest)
        val cbMain = dialogView.findViewById<CheckBox>(com.example.lifegame.R.id.cb_main_quest)
        val cbSide = dialogView.findViewById<CheckBox>(com.example.lifegame.R.id.cb_side_quest)
        val cbWeekly = dialogView.findViewById<CheckBox>(com.example.lifegame.R.id.cb_weekly_quest)
        
        cbDaily.isChecked = settings["daily"] ?: false
        cbMain.isChecked = settings["main"] ?: true
        cbSide.isChecked = settings["side"] ?: true
        cbWeekly.isChecked = settings["weekly"] ?: false

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("日志默认锁定设置")
            .setMessage("开启后，对应类型任务产生的日志将默认锁定，不会被自动清理。")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                viewModel.setDefaultLockForQuestType(0, cbDaily.isChecked)
                viewModel.setDefaultLockForQuestType(1, cbMain.isChecked)
                viewModel.setDefaultLockForQuestType(2, cbSide.isChecked)
                viewModel.setDefaultLockForQuestType(3, cbWeekly.isChecked)
                Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun observeData() {
        super.observeData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allLogs.collect { logs ->
                    logAdapter.submitList(logs)
                }
            }
        }
    }
}
