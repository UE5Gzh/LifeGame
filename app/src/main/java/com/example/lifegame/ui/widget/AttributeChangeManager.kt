package com.example.lifegame.ui.widget

import android.content.Context
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
    private var isShowing = false
    private var currentView: AttributeChangeView? = null
    private var currentIndex = 0

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
        
        if (!isShowing) {
            showNext()
        }
    }

    fun showAttributeChangesNamed(
        changes: List<AttributeChangeItem>
    ) {
        val filteredChanges = changes.filter { it.changeValue != 0f }
        if (filteredChanges.isEmpty()) return
        
        changeQueue.addAll(filteredChanges)
        
        if (!isShowing) {
            showNext()
        }
    }

    private fun showNext() {
        if (changeQueue.isEmpty()) {
            isShowing = false
            currentIndex = 0
            return
        }
        
        isShowing = true
        val item = changeQueue.removeAt(0)
        
        val view = AttributeChangeView(context)
        view.visibility = View.INVISIBLE
        
        container.post {
            val screenHeight = container.height
            val baseBottomMargin = (screenHeight * 0.33f).toInt()
            val verticalSpacing = 120
            val bottomMargin = baseBottomMargin + (currentIndex * verticalSpacing)
            
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                this.bottomMargin = bottomMargin
            }
            container.addView(view, params)
            view.visibility = View.VISIBLE
            
            currentView = view
            currentIndex++
            
            view.showAttributeChange(item.attributeName, item.changeValue, item.colorHex) {
                container.removeView(view)
                currentView = null
                
                scope.launch {
                    delay(150)
                    showNext()
                }
            }
        }
    }

    fun clear() {
        currentView?.cancel()
        changeQueue.clear()
        isShowing = false
        currentIndex = 0
        container.removeAllViews()
    }

    fun destroy() {
        clear()
        scope.cancel()
    }
}
