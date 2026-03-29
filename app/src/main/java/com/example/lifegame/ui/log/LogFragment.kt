package com.example.lifegame.ui.log

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.example.lifegame.data.entity.LogEntity
import com.example.lifegame.databinding.DialogConfirmBinding
import com.example.lifegame.databinding.DialogAppSettingsBinding
import com.example.lifegame.databinding.DialogDefaultLockSettingsBinding
import com.example.lifegame.databinding.DialogExportSuccessBinding
import com.example.lifegame.databinding.DialogOptionsBinding
import com.example.lifegame.databinding.DialogStorageSettingsBinding
import com.example.lifegame.databinding.DialogWriteLogBinding
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
            val dialogBinding = DialogConfirmBinding.inflate(LayoutInflater.from(requireContext()))
            dialogBinding.tvTitle.text = "清空日志"
            dialogBinding.tvMessage.text = "确定要清空所有未锁定的日志记录吗？此操作不可撤销。"

            val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialogBinding.btnConfirm.setOnClickListener {
                viewModel.clearLogs()
                dialog.dismiss()
            }

            dialog.show()
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
        
        val dialogBinding = DialogOptionsBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.tvTitle.text = "日志操作"
        
        dialogBinding.btnOption1.text = "编辑日志"
        dialogBinding.cardOption1.visibility = View.VISIBLE

        dialogBinding.btnOption2.text = "删除此日志"
        dialogBinding.cardOption2.visibility = View.VISIBLE

        dialogBinding.btnOption3.text = lockText
        dialogBinding.cardOption3.visibility = View.VISIBLE

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.cardOption1.setOnClickListener {
            showEditLogDialog(log)
            dialog.dismiss()
        }

        dialogBinding.cardOption2.setOnClickListener {
            viewModel.deleteLog(log)
            dialog.dismiss()
        }

        dialogBinding.cardOption3.setOnClickListener {
            viewModel.toggleLogLock(log)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddLogDialog() {
        val dialogBinding = DialogWriteLogBinding.inflate(LayoutInflater.from(requireContext()))
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        dialogBinding.tvDate.text = dateFormat.format(Date())

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val content = dialogBinding.etContent.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.insertCustomLog(content)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditLogDialog(log: LogEntity) {
        val dialogBinding = DialogWriteLogBinding.inflate(LayoutInflater.from(requireContext()))
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        
        dialogBinding.tvDialogTitle.text = "编辑日志"
        dialogBinding.tvDate.text = dateFormat.format(Date(log.timestamp))
        dialogBinding.etContent.setText(log.details)
        dialogBinding.etContent.setSelection(log.details.length)
        dialogBinding.tvHint.text = if (log.isLocked) "日志已锁定" else "日志未锁定"

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val newContent = dialogBinding.etContent.text.toString().trim()
            if (newContent.isNotEmpty()) {
                val updatedLog = log.copy(details = newContent)
                viewModel.updateLog(updatedLog)
            }
            dialog.dismiss()
        }

        dialog.show()
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
            showNoLogFoundDialog(targetDateStr)
        }
    }

    private fun showNoLogFoundDialog(dateStr: String) {
        val dialogBinding = DialogConfirmBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.tvTitle.text = "未找到日志"
        dialogBinding.tvMessage.text = "在 $dateStr 没有找到任何日志记录"

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.visibility = View.GONE
        dialogBinding.btnConfirm.text = "知道了"
        dialogBinding.btnConfirm.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val dialogBinding = DialogAppSettingsBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        // 设置状态触发日志开关
        dialogBinding.switchStatusLog.isChecked = viewModel.isStatusTriggerLogEnabled()
        dialogBinding.switchStatusLog.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setStatusTriggerLogEnabled(isChecked)
        }

        dialogBinding.btnExport.setOnClickListener {
            exportLogs()
            dialog.dismiss()
        }

        dialogBinding.btnStorage.setOnClickListener {
            showStorageSettingsDialog()
            dialog.dismiss()
        }

        dialogBinding.btnDefaultLock.setOnClickListener {
            showDefaultLockSettingsDialog()
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportLogs() {
        val logs = logAdapter.currentList
        if (logs.isEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val file = LogExportHelper.exportLogsToCsv(requireContext(), logs)
            if (file != null) {
                val dialogBinding = DialogExportSuccessBinding.inflate(LayoutInflater.from(requireContext()))
                dialogBinding.tvFilename.text = file.name

                val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setView(dialogBinding.root)
                    .create()

                dialogBinding.btnLater.setOnClickListener {
                    dialog.dismiss()
                }

                dialogBinding.btnShare.setOnClickListener {
                    LogExportHelper.shareFile(requireContext(), file)
                    dialog.dismiss()
                }

                dialog.show()
            }
        }
    }

    private fun showStorageSettingsDialog() {
        val currentLimit = viewModel.getMaxLogLimit()
        val dialogBinding = DialogStorageSettingsBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.etLimit.setText(currentLimit.toString())
        dialogBinding.etLimit.setSelection(dialogBinding.etLimit.text?.length ?: 0)

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val inputStr = dialogBinding.etLimit.text.toString()
            val limit = inputStr.toIntOrNull()
            
            if (limit != null && limit in 50..50000) {
                viewModel.setMaxLogLimit(limit)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDefaultLockSettingsDialog() {
        val settings = viewModel.getDefaultLockSettings()
        val dialogBinding = DialogDefaultLockSettingsBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.switchDailyQuest.isChecked = settings["daily"] ?: false
        dialogBinding.switchMainQuest.isChecked = settings["main"] ?: true
        dialogBinding.switchSideQuest.isChecked = settings["side"] ?: true
        dialogBinding.switchWeeklyQuest.isChecked = settings["weekly"] ?: false

        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.setDefaultLockForQuestType(0, dialogBinding.switchDailyQuest.isChecked)
            viewModel.setDefaultLockForQuestType(1, dialogBinding.switchMainQuest.isChecked)
            viewModel.setDefaultLockForQuestType(2, dialogBinding.switchSideQuest.isChecked)
            viewModel.setDefaultLockForQuestType(3, dialogBinding.switchWeeklyQuest.isChecked)
            dialog.dismiss()
        }

        dialog.show()
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
