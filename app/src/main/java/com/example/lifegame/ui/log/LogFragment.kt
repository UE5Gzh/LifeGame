package com.example.lifegame.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.EditText
import android.widget.Toast
import com.example.lifegame.data.entity.LogEntity
import com.example.lifegame.databinding.FragmentLogBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

    private fun showSettingsDialog() {
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
