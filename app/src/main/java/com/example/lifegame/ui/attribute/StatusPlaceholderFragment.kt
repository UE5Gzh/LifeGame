package com.example.lifegame.ui.attribute

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.databinding.DialogDeleteConfirmBinding
import com.example.lifegame.databinding.FragmentStatusPlaceholderBinding
import com.example.lifegame.ui.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
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
                    viewModel.statusesWithEffects.collect { statusesWithEffects ->
                        if (::adapter.isInitialized) {
                            adapter.updateAttributes(viewModel.attributes.value)
                            adapter.submitList(statusesWithEffects)
                        }
                        binding.tvEmpty.visibility = if (statusesWithEffects.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                
                launch {
                    viewModel.attributes.collect { attributes ->
                        if (::adapter.isInitialized) {
                            adapter.updateAttributes(attributes)
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
            onItemClick = { status ->
                showEditStatusDialog(status)
            },
            onItemLongClick = { status ->
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
        } else {
            itemTouchHelper?.attachToRecyclerView(null)
            saveSortOrder()
        }
    }

    private fun saveSortOrder() {
        val updatedStatuses = adapter.getStatusesInOrder().mapIndexed { index, status ->
            status.copy(sortOrder = index)
        }
        viewModel.updateStatusSortOrders(updatedStatuses)
    }

    private fun showEditStatusDialog(status: StatusEntity) {
        (parentFragment as? AttributeFragment)?.showAddStatusDialog(status)
    }

    private fun showDeleteConfirmDialog(status: StatusEntity) {
        val dialogBinding = DialogDeleteConfirmBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvTitle.text = "删除状态"
        dialogBinding.tvMessage.text = "确定要删除状态「${status.name}」吗？\n删除后相关数据将无法恢复。"

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.deleteStatus(status)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun triggerAddStatus() {
        (parentFragment as? AttributeFragment)?.showAddStatusDialog(null)
    }

    fun triggerSortMode() {
        toggleSortMode()
    }

    fun isInSortMode(): Boolean = isSortMode
}
