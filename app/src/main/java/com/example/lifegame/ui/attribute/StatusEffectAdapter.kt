package com.example.lifegame.ui.attribute

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lifegame.R
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.databinding.ItemStatusEffectBinding
import kotlin.math.roundToInt

data class EffectItem(
    var effectType: Int = 0,
    var targetAttributeId: Long = 0,
    var periodValue: Int = 1,
    var periodUnit: Int = 0,
    var changeValue: Float = 0f,
    var bonusPercent: Float = 0f,
    var sortOrder: Int = 0
) {
    fun toStatusEffectEntity(statusId: Long, order: Int): StatusEffectEntity {
        return StatusEffectEntity(
            statusId = statusId,
            effectType = effectType,
            targetAttributeId = targetAttributeId,
            periodValue = periodValue,
            periodUnit = periodUnit,
            changeValue = changeValue,
            bonusPercent = bonusPercent,
            sortOrder = order
        )
    }
    
    companion object {
        fun fromStatusEffectEntity(entity: StatusEffectEntity): EffectItem {
            return EffectItem(
                effectType = entity.effectType,
                targetAttributeId = entity.targetAttributeId,
                periodValue = entity.periodValue,
                periodUnit = entity.periodUnit,
                changeValue = entity.changeValue,
                bonusPercent = entity.bonusPercent,
                sortOrder = entity.sortOrder
            )
        }
    }
}

class StatusEffectAdapter(
    private var attributes: List<AttributeWithRanks>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<StatusEffectAdapter.EffectViewHolder>() {

    private val effects = mutableListOf<EffectItem>()

    fun setEffects(newEffects: List<EffectItem>) {
        effects.clear()
        effects.addAll(newEffects)
        notifyDataSetChanged()
    }

    fun addEffect(effect: EffectItem) {
        effects.add(effect)
        notifyItemInserted(effects.size - 1)
    }

    fun removeEffect(position: Int) {
        if (position in 0 until effects.size) {
            effects.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, effects.size)
        }
    }

    fun getEffects(): List<EffectItem> = effects.toList()

    fun updateAttributes(newAttributes: List<AttributeWithRanks>) {
        attributes = newAttributes
        notifyDataSetChanged()
    }

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    inner class EffectViewHolder(private val binding: ItemStatusEffectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isBinding = false
        private var periodValueWatcher: TextWatcher? = null
        private var changeValueWatcher: TextWatcher? = null
        private var bonusPercentWatcher: TextWatcher? = null
        private var decayPercentWatcher: TextWatcher? = null

        fun bind(effect: EffectItem, position: Int) {
            isBinding = true
            
            val typeText = when (effect.effectType) {
                0 -> "周期性变动"
                1 -> "属性加成"
                else -> "属性衰减"
            }
            binding.tvEffectType.text = typeText

            setupAttributeSpinner(effect, position)
            setupPeriodUnitSpinner(position)

            removeTextWatchers()

            when (effect.effectType) {
                0 -> {
                    binding.llPeriodicParams.visibility = View.VISIBLE
                    binding.llBonusParams.visibility = View.GONE
                    binding.llDecayParams.visibility = View.GONE
                    binding.etPeriodValue.setText(effect.periodValue.toString())
                    binding.spinnerPeriodUnit.setSelection(effect.periodUnit)
                    binding.etChangeValue.setText(formatValue(effect.changeValue))
                    
                    periodValueWatcher = createIntTextWatcher(position) { value ->
                        if (position in effects.indices) {
                            effects[position].periodValue = value
                        }
                    }
                    changeValueWatcher = createFloatTextWatcher(position) { value ->
                        if (position in effects.indices) {
                            effects[position].changeValue = value
                        }
                    }
                    binding.etPeriodValue.addTextChangedListener(periodValueWatcher)
                    binding.etChangeValue.addTextChangedListener(changeValueWatcher)
                }
                1 -> {
                    binding.llPeriodicParams.visibility = View.GONE
                    binding.llBonusParams.visibility = View.VISIBLE
                    binding.llDecayParams.visibility = View.GONE
                    binding.etBonusPercent.setText(formatValue(effect.bonusPercent))
                    
                    bonusPercentWatcher = createFloatTextWatcher(position) { value ->
                        if (position in effects.indices) {
                            effects[position].bonusPercent = value
                        }
                    }
                    binding.etBonusPercent.addTextChangedListener(bonusPercentWatcher)
                }
                else -> {
                    binding.llPeriodicParams.visibility = View.GONE
                    binding.llBonusParams.visibility = View.GONE
                    binding.llDecayParams.visibility = View.VISIBLE
                    binding.etDecayPercent.setText(formatValue(effect.bonusPercent))
                    
                    decayPercentWatcher = createFloatTextWatcher(position) { value ->
                        if (position in effects.indices) {
                            effects[position].bonusPercent = value
                        }
                    }
                    binding.etDecayPercent.addTextChangedListener(decayPercentWatcher)
                }
            }

            binding.btnDeleteEffect.setOnClickListener {
                onDeleteClick(position)
            }

            isBinding = false
        }

        private fun removeTextWatchers() {
            periodValueWatcher?.let { binding.etPeriodValue.removeTextChangedListener(it) }
            changeValueWatcher?.let { binding.etChangeValue.removeTextChangedListener(it) }
            bonusPercentWatcher?.let { binding.etBonusPercent.removeTextChangedListener(it) }
            decayPercentWatcher?.let { binding.etDecayPercent.removeTextChangedListener(it) }
        }

        private fun createIntTextWatcher(position: Int, onUpdate: (Int) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isBinding) return
                    val value = s?.toString()?.toIntOrNull() ?: 1
                    onUpdate(value)
                }
            }
        }

        private fun createFloatTextWatcher(position: Int, onUpdate: (Float) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isBinding) return
                    val value = s?.toString()?.toFloatOrNull() ?: 0f
                    onUpdate(value)
                }
            }
        }

        private fun setupAttributeSpinner(effect: EffectItem, position: Int) {
            if (attributes.isNotEmpty()) {
                val attrNames = attributes.map { it.attribute.name }
                val adapter = ArrayAdapter(binding.root.context, R.layout.spinner_item_dark, attrNames)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
                binding.spinnerAttribute.adapter = adapter

                val attrIndex = attributes.indexOfFirst { it.attribute.id == effect.targetAttributeId }
                if (attrIndex >= 0) {
                    binding.spinnerAttribute.setSelection(attrIndex)
                } else if (attributes.isNotEmpty()) {
                    if (position in effects.indices) {
                        effects[position].targetAttributeId = attributes[0].attribute.id
                    }
                }

                binding.spinnerAttribute.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (isBinding) return
                        if (pos in attributes.indices && position in effects.indices) {
                            effects[position].targetAttributeId = attributes[pos].attribute.id
                        }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                })
            }
        }

        private fun setupPeriodUnitSpinner(position: Int) {
            val units = arrayOf("小时", "天")
            val adapter = ArrayAdapter(binding.root.context, R.layout.spinner_item_dark, units)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
            binding.spinnerPeriodUnit.adapter = adapter

            binding.spinnerPeriodUnit.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    if (isBinding) return
                    if (position in effects.indices) {
                        effects[position].periodUnit = pos
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val binding = ItemStatusEffectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EffectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        holder.bind(effects[position], position)
    }

    override fun getItemCount(): Int = effects.size
}
