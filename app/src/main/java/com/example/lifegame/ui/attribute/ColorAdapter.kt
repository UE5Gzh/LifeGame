package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.databinding.ItemColorBinding
import com.example.lifegame.utils.ColorPalette

class ColorAdapter(
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private val colors = ColorPalette.darkColors
    private var selectedPosition = 0

    init {
        onColorSelected(colors[selectedPosition])
    }

    inner class ColorViewHolder(val binding: ItemColorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(colorHex: String, position: Int) {
            binding.cardColor.setCardBackgroundColor(Color.parseColor(colorHex))
            
            if (position == selectedPosition) {
                binding.ivCheck.visibility = View.VISIBLE
            } else {
                binding.ivCheck.visibility = View.GONE
            }

            if (position == 0) {
                binding.tvDefault.visibility = View.VISIBLE
            } else {
                binding.tvDefault.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onColorSelected(colorHex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        return ColorViewHolder(
            ItemColorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position)
    }

    override fun getItemCount(): Int = colors.size
}
