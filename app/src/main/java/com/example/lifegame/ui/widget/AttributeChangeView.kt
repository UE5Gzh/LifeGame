package com.example.lifegame.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.lifegame.R
import kotlin.math.abs

class AttributeChangeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textView: TextView
    private var animator: ValueAnimator? = null

    init {
        textView = TextView(context).apply {
            textSize = 18f
            setPadding(24, 12, 24, 12)
            setBackgroundResource(R.drawable.bg_attribute_change)
        }
        addView(textView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
        clipChildren = false
        clipToPadding = false
    }

    fun showAttributeChange(attributeName: String, changeValue: Float, colorHex: String?, onComplete: () -> Unit) {
        animator?.cancel()
        
        val sign = if (changeValue >= 0) "+" else ""
        val valueText = if (changeValue == changeValue.toInt().toFloat()) {
            "${changeValue.toInt()}"
        } else {
            String.format("%.1f", changeValue)
        }
        
        textView.text = "$attributeName $sign$valueText"
        
        val textColor = if (!colorHex.isNullOrEmpty()) {
            try {
                Color.parseColor(colorHex)
            } catch (e: Exception) {
                getDefaultColor(changeValue)
            }
        } else {
            getDefaultColor(changeValue)
        }
        
        textView.setTextColor(textColor)
        
        alpha = 0f
        translationY = 100f
        scaleX = 0.5f
        scaleY = 0.5f
        
        val fadeIn = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                alpha = value
                scaleX = 0.5f + 0.5f * value
                scaleY = 0.5f + 0.5f * value
                translationY = 100f * (1f - value)
            }
        }
        
        val fadeOut = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500
            startDelay = 1500
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                alpha = value
                translationY = 100f * (1f - value) - 50f * (1f - value)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
        }
        
        fadeIn.start()
        fadeOut.start()
        animator = fadeOut
    }
    
    private fun getDefaultColor(changeValue: Float): Int {
        return if (changeValue >= 0) {
            ContextCompat.getColor(context, R.color.attribute_positive)
        } else {
            ContextCompat.getColor(context, R.color.attribute_negative)
        }
    }

    fun cancel() {
        animator?.cancel()
        animator = null
    }
}
