package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R

class ColorPickerAdapter(
    private val colors: List<String>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    var selectedPosition = 0
        set(value) {
            val oldPosition = field
            field = value
            notifyItemChanged(oldPosition)
            notifyItemChanged(value)
        }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorView: View = itemView.findViewById(R.id.color_view)
        private val checkMark: ImageView = itemView.findViewById(R.id.check_mark)

        fun bind(colorHex: String, position: Int) {
            try {
                val color = Color.parseColor(colorHex)
                (colorView.background as? GradientDrawable)?.setColor(color)
            } catch (e: Exception) {
                (colorView.background as? GradientDrawable)?.setColor(Color.parseColor("#21212B"))
            }

            checkMark.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                selectedPosition = position
                onColorSelected(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_picker, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position)
    }

    override fun getItemCount(): Int = colors.size
}
