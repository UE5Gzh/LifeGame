package com.example.lifegame.ui.attribute

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lifegame.databinding.DialogAddAttributeBinding
import com.example.lifegame.databinding.FragmentAttributeBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttributeFragment : BaseFragment<FragmentAttributeBinding>() {

    private val viewModel: AttributeViewModel by viewModels()
    private lateinit var adapter: AttributeAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAttributeBinding {
        return FragmentAttributeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        super.setupViews()
        setupRecyclerView()
        
        binding.btnAdd.setOnClickListener {
            showAddAttributeDialog()
        }

        binding.btnInit.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    override fun observeData() {
        super.observeData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.attributes.collect { attributes ->
                    adapter.submitList(attributes)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AttributeAdapter()
        binding.rvAttributes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttributes.adapter = adapter
    }

    private fun showAddAttributeDialog() {
        val dialogBinding = DialogAddAttributeBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        // Setup color picker
        var selectedColor = ""
        val colorAdapter = ColorAdapter { colorHex ->
            selectedColor = colorHex
        }
        dialogBinding.rvColors.layoutManager = GridLayoutManager(requireContext(), 6)
        dialogBinding.rvColors.adapter = colorAdapter

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etName.text?.toString()?.trim()
            val initialValueStr = dialogBinding.etInitialValue.text?.toString()?.trim()

            if (name.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "请输入属性名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val initialValue = initialValueStr?.toIntOrNull() ?: 10

            viewModel.addAttribute(name, initialValue, selectedColor)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("重置属性")
            .setMessage("确定要将所有属性重置为初始数值吗？此操作不可撤销。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.resetAllAttributes()
                Toast.makeText(requireContext(), "已重置所有属性", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

