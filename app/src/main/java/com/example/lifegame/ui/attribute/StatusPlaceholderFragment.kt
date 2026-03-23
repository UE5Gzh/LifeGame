package com.example.lifegame.ui.attribute

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.databinding.FragmentStatusPlaceholderBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatusPlaceholderFragment : BaseFragment<FragmentStatusPlaceholderBinding>() {

    private val viewModel: StatusViewModel by activityViewModels()
    private lateinit var adapter: StatusAdapter
    private var isSortMode = false
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStatusPlaceholderBinding {
        return FragmentStatusPlaceholderBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        
        binding.root.setOnLongClickListener {
            toggleSortMode()
            true
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.statuses.collect { statuses ->
                        adapter = StatusAdapter(
                            attributes = viewModel.attributes.value,
                            onStatusToggle = { status, enabled ->
                                viewModel.toggleStatus(status, enabled)
                            },
                            onEditClick = { status ->
                                showEditStatusDialog(status)
                            },
                            onDeleteClick = { status ->
                                showDeleteConfirmDialog(status)
                            }
                        )
                        adapter.isSortMode = isSortMode
                        binding.rvStatuses.adapter = adapter
                        adapter.submitList(statuses)
                        
                        binding.tvEmpty.visibility = if (statuses.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                
                launch {
                    viewModel.attributes.collect { attributes ->
                        if (::adapter.isInitialized) {
                            adapter = StatusAdapter(
                                attributes = attributes,
                                onStatusToggle = { status, enabled ->
                                    viewModel.toggleStatus(status, enabled)
                                },
                                onEditClick = { status ->
                                    showEditStatusDialog(status)
                                },
                                onDeleteClick = { status ->
                                    showDeleteConfirmDialog(status)
                                }
                            )
                            adapter.isSortMode = isSortMode
                            binding.rvStatuses.adapter = adapter
                            adapter.submitList(viewModel.statuses.value)
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StatusAdapter(
            attributes = emptyList(),
            onStatusToggle = { status, enabled ->
                viewModel.toggleStatus(status, enabled)
            },
            onEditClick = { status ->
                showEditStatusDialog(status)
            },
            onDeleteClick = { status ->
                showDeleteConfirmDialog(status)
            }
        )
        binding.rvStatuses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatuses.adapter = adapter

        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.swapItems(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean {
                return isSortMode
            }
        }
        itemTouchHelper = ItemTouchHelper(touchHelperCallback)
    }

    private fun toggleSortMode() {
        isSortMode = !isSortMode
        adapter.isSortMode = isSortMode
        if (isSortMode) {
            itemTouchHelper?.attachToRecyclerView(binding.rvStatuses)
            Toast.makeText(requireContext(), "进入排序模式，长按保存", Toast.LENGTH_SHORT).show()
        } else {
            itemTouchHelper?.attachToRecyclerView(null)
            saveSortOrder()
        }
    }

    private fun saveSortOrder() {
        val currentList = adapter.currentList
        val updatedStatuses = currentList.mapIndexed { index, status ->
            status.copy(sortOrder = index)
        }
        viewModel.updateStatusSortOrders(updatedStatuses)
        Toast.makeText(requireContext(), "排序已保存", Toast.LENGTH_SHORT).show()
    }

    private fun showEditStatusDialog(status: StatusEntity) {
        (parentFragment as? AttributeFragment)?.showAddStatusDialog(status)
    }

    private fun showDeleteConfirmDialog(status: StatusEntity) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("删除状态")
            .setMessage("确定要删除状态「${status.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteStatus(status)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun triggerAddStatus() {
        (parentFragment as? AttributeFragment)?.showAddStatusDialog(null)
    }

    fun triggerSortMode() {
        toggleSortMode()
    }

    fun isInSortMode(): Boolean = isSortMode
}
