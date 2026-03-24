package com.example.lifegame.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.example.lifegame.R

class CelebrationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tvTitle: TextView
    private val tvSubtitle: TextView
    private var animatorSet: AnimatorSet? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_celebration, this, true)
        tvTitle = view.findViewById(R.id.tv_celebration_title)
        tvSubtitle = view.findViewById(R.id.tv_celebration_subtitle)
        clipChildren = false
        clipToPadding = false
    }

    fun showRankUp(attributeName: String, oldRank: String, newRank: String, onComplete: () -> Unit) {
        tvTitle.text = "🎉 段位提升！"
        tvSubtitle.text = "$attributeName\n$oldRank → $newRank"
        showCelebration(onComplete)
    }

    fun showQuestComplete(questName: String, questType: String, onComplete: () -> Unit) {
        tvTitle.text = "🏆 任务完成！"
        tvSubtitle.text = "$questType\n$questName"
        showCelebration(onComplete)
    }

    private fun showCelebration(onComplete: () -> Unit) {
        animatorSet?.cancel()
        
        alpha = 0f
        translationY = 200f
        scaleX = 0.3f
        scaleY = 0.3f
        visibility = VISIBLE
        
        val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(1.5f)
        }
        
        val scaleUpX = ObjectAnimator.ofFloat(this, "scaleX", 0.3f, 1f).apply {
            duration = 500
            interpolator = BounceInterpolator()
        }
        
        val scaleUpY = ObjectAnimator.ofFloat(this, "scaleY", 0.3f, 1f).apply {
            duration = 500
            interpolator = BounceInterpolator()
        }
        
        val slideUp = ObjectAnimator.ofFloat(this, "translationY", 200f, 0f).apply {
            duration = 500
            interpolator = OvershootInterpolator(0.5f)
        }
        
        val pulseScaleX = ValueAnimator.ofFloat(1f, 1.1f, 1f).apply {
            duration = 600
            repeatCount = 2
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animation ->
                scaleX = animation.animatedValue as Float
            }
        }
        
        val pulseScaleY = ValueAnimator.ofFloat(1f, 1.1f, 1f).apply {
            duration = 600
            repeatCount = 2
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animation ->
                scaleY = animation.animatedValue as Float
            }
        }
        
        val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            duration = 500
            startDelay = 2000
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val scaleDown = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.8f).apply {
            duration = 500
            startDelay = 2000
        }
        
        animatorSet = AnimatorSet().apply {
            playTogether(fadeIn, scaleUpX, scaleUpY, slideUp)
            play(pulseScaleX).after(500)
            play(pulseScaleY).after(500)
            play(fadeOut).after(2500)
            play(scaleDown).after(2500)
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                    onComplete()
                }
            })
            start()
        }
    }

    fun cancel() {
        animatorSet?.cancel()
        animatorSet = null
        visibility = GONE
    }
}
