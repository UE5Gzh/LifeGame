package com.example.lifegame.ui.widget

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import com.example.lifegame.util.CelebrationEvent
import com.example.lifegame.util.CelebrationType
import kotlinx.coroutines.*

class CelebrationManager(
    private val context: Context,
    private val container: FrameLayout
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val eventQueue = mutableListOf<CelebrationEvent>()
    private var isShowing = false
    private var currentView: CelebrationView? = null
    private var skipRequested = false

    fun showCelebration(event: CelebrationEvent) {
        eventQueue.add(event)
        eventQueue.sortBy { it.priority }
        
        if (!isShowing) {
            showNext()
        }
    }

    private fun showNext() {
        if (eventQueue.isEmpty()) {
            isShowing = false
            return
        }

        isShowing = true
        skipRequested = false
        val event = eventQueue.removeAt(0)

        val view = CelebrationView(context)
        view.visibility = android.view.View.INVISIBLE

        container.post {
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            container.addView(view, params)
            view.visibility = android.view.View.VISIBLE

            currentView = view

            when (event.type) {
                CelebrationType.RANK_UP -> {
                    view.showRankUp(event.attributeName, event.oldRank, event.newRank) {
                        onAnimationComplete(view)
                    }
                }
                else -> {
                    view.showQuestComplete(event.questName, event.questType) {
                        onAnimationComplete(view)
                    }
                }
            }
        }
        
        container.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isShowing) {
                skipRequested = true
                currentView?.cancel()
                true
            } else {
                false
            }
        }
    }
    
    private fun onAnimationComplete(view: CelebrationView) {
        container.removeView(view)
        currentView = null
        
        scope.launch {
            delay(200)
            showNext()
        }
    }

    fun clear() {
        currentView?.cancel()
        eventQueue.clear()
        isShowing = false
        skipRequested = false
        container.removeAllViews()
        container.setOnTouchListener(null)
    }

    fun destroy() {
        clear()
        scope.cancel()
    }
}
