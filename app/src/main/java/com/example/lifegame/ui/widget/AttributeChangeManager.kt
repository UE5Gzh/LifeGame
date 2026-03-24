package com.example.lifegame.ui.widget

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.util.AttributeChangeItem
import kotlinx.coroutines.*

class AttributeChangeManager(
    private val context: Context,
    private val container: FrameLayout
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val changeQueue = mutableListOf<AttributeChangeItem>()
    private val activeViews = mutableListOf<AttributeChangeView>()
    private var isProcessing = false
    private val maxVisibleAnimations = 3
    private val animationDelay = 200L

    fun showAttributeChanges(
        changes: List<Pair<Long, Float>>,
        attributes: List<AttributeWithRanks>
    ) {
        val namedChanges = changes.mapNotNull { (attrId, change) ->
            val attr = attributes.find { it.attribute.id == attrId }
            if (attr != null && change != 0f) {
                AttributeChangeItem(attr.attribute.name, change, attr.attribute.colorHex)
            } else {
                null
            }
        }
        
        if (namedChanges.isEmpty()) return
        
        changeQueue.addAll(namedChanges)
        
        if (!isProcessing) {
            processQueue()
        }
    }

    fun showAttributeChangesNamed(
        changes: List<AttributeChangeItem>
    ) {
        val filteredChanges = changes.filter { it.changeValue != 0f }
        if (filteredChanges.isEmpty()) return
        
        changeQueue.addAll(filteredChanges)
        
        if (!isProcessing) {
            processQueue()
        }
    }

    private fun processQueue() {
        if (changeQueue.isEmpty() && activeViews.isEmpty()) {
            isProcessing = false
            return
        }
        
        isProcessing = true
        
        if (changeQueue.isNotEmpty() && activeViews.size < maxVisibleAnimations) {
            val item = changeQueue.removeAt(0)
            showAnimation(item)
            
            scope.launch {
                delay(animationDelay)
                processQueue()
            }
        }
    }

    private fun showAnimation(item: AttributeChangeItem) {
        val view = AttributeChangeView(context)
        view.visibility = View.INVISIBLE
        
        container.post {
            val bottomNavHeight = getBottomNavHeight()
            val baseBottomMargin = bottomNavHeight + dpToPx(30)
            val verticalSpacing = dpToPx(60)
            val bottomMargin = baseBottomMargin + (activeViews.size * verticalSpacing)
            
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                this.bottomMargin = bottomMargin
            }
            container.addView(view, params)
            view.visibility = View.VISIBLE
            
            activeViews.add(view)
            
            view.showAttributeChange(item.attributeName, item.changeValue, item.colorHex) {
                container.removeView(view)
                activeViews.remove(view)
                
                if (changeQueue.isEmpty() && activeViews.isEmpty()) {
                    isProcessing = false
                } else if (changeQueue.isNotEmpty() && activeViews.size < maxVisibleAnimations) {
                    processQueue()
                }
            }
        }
    }

    private fun getBottomNavHeight(): Int {
        val activity = context.findActivity()
        val navView = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.example.lifegame.R.id.bottom_navigation)
        return navView?.height ?: dpToPx(56)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun Context.findActivity(): android.app.Activity? {
        var context = this
        while (context is android.content.ContextWrapper) {
            if (context is android.app.Activity) return context
            context = context.baseContext
        }
        return null
    }

    fun clear() {
        activeViews.forEach { it.cancel() }
        activeViews.clear()
        changeQueue.clear()
        isProcessing = false
        container.removeAllViews()
    }

    fun destroy() {
        clear()
        scope.cancel()
    }
}
