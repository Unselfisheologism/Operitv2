package com.ai.assistance.operit.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.ui.automation.*
import kotlinx.coroutines.*
import android.graphics.Rect

/**
 * OperitAccessibilityService
 * Provides UI automation capabilities through Android AccessibilityService API
 */
class OperitAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "OperitAccessibilityService"
        
        @Volatile
        var instance: OperitAccessibilityService? = null
        
        fun getInstance(): OperitAccessibilityService? = instance
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentActivityName: String? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.d(TAG, "Accessibility service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentActivityName = event.packageName?.toString()
                AppLogger.d(TAG, "Activity changed to: $currentActivityName")
            }
        }
    }
    
    override fun onInterrupt() {
        AppLogger.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        AppLogger.d(TAG, "Accessibility service destroyed")
    }
    
    /**
     * Get UI hierarchy as XML
     */
    fun getUIHierarchyXml(): String {
        return try {
            val rootNode = rootInActiveWindow ?: return ""
            val parser = SemanticParser()
            val analysis = parser.parseNodeTree(rootNode)
            rootNode.recycle()
            analysis.uiRepresentation
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting UI hierarchy", e)
            ""
        }
    }
    
    /**
     * Simplified UI hierarchy
     */
    fun getSimplifiedUIHierarchy(): String {
        return try {
            val rootNode = rootInActiveWindow ?: return ""
            
            val sb = StringBuilder()
            dumpSimplifiedNode(rootNode, sb, 0)
            
            rootNode.recycle()
            sb.toString()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting simplified hierarchy", e)
            ""
        }
    }
    
    private fun dumpSimplifiedNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (!node.isVisibleToUser) return
        
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val resourceId = node.resourceId?.toString() ?: ""
        
        if (text.isNotBlank() || contentDesc.isNotBlank() || node.isClickable) {
            val indent = "  ".repeat(depth)
            sb.appendLine("$indent- ${node.className?.simpleName}: $text (id: $resourceId)")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpSimplifiedNode(child, sb, depth + 1)
                child.recycle()
            }
        }
    }
    
    /**
     * Execute click at coordinates
     */
    fun performClickAt(x: Int, y: Int): Boolean {
        AppLogger.d(TAG, "Click at: ($x, $y)")
        return performClick(x.toFloat(), y.toFloat())
    }
    
    private fun performClick(x: Float, y: Float): Boolean {
        val path = android.graphics.Path().apply {
            moveTo(x, y)
        }
        
        val builder = android.view.accessibility.AccessibilityGestureDescription.Builder(1)
        builder.addStroke(0, 500, path)
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    /**
     * Execute long press at coordinates
     */
    fun performLongPressAt(x: Int, y: Int): Boolean {
        AppLogger.d(TAG, "Long press at: ($x, $y)")
        return performLongClick(x.toFloat(), y.toFloat())
    }
    
    private fun performLongClick(x: Float, y: Float): Boolean {
        val path = android.graphics.Path().apply {
            moveTo(x, y)
        }
        
        val builder = android.view.accessibility.AccessibilityGestureDescription.Builder(1)
        builder.addStroke(0, 1500, path)
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    /**
     * Execute swipe gesture
     */
    fun performSwipeGesture(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        AppLogger.d(TAG, "Swipe: ($startX, $startY) -> ($endX, $endY), duration=$duration")
        return performSwipe(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), duration)
    }
    
    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        val path = android.graphics.Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        
        val builder = android.view.accessibility.AccessibilityGestureDescription.Builder(1)
        builder.addStroke(0, duration, path)
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    /**
     * Execute scroll down
     */
    fun performScrollDown(): Boolean {
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        display.getRealMetrics(metrics)
        
        val startY = (metrics.heightPixels * 0.8).toInt()
        val endY = (metrics.heightPixels * 0.2).toInt()
        val centerX = metrics.widthPixels / 2
        
        return performSwipe(centerX.toFloat(), startY.toFloat(), centerX.toFloat(), endY.toFloat(), 500)
    }
    
    /**
     * Execute scroll up
     */
    fun performScrollUp(): Boolean {
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        display.getRealMetrics(metrics)
        
        val startY = (metrics.heightPixels * 0.2).toInt()
        val endY = (metrics.heightPixels * 0.8).toInt()
        val centerX = metrics.widthPixels / 2
        
        return performSwipe(centerX.toFloat(), startY.toFloat(), centerX.toFloat(), endY.toFloat(), 500)
    }
    
    /**
     * Input text
     */
    fun performTextInput(text: String): Boolean {
        AppLogger.d(TAG, "Input text: $text")
        
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUSABLE)
        
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            return result
        }
        
        focusedNode?.recycle()
        return false
    }
    
    /**
     * Get current activity
     */
    fun getCurrentActivity(): String? = currentActivityName
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = instance != null
    
    /**
     * Get interactive elements
     */
    fun getInteractiveElements(): Map<Int, InteractiveElement> {
        return try {
            val rootNode = rootInActiveWindow ?: return emptyMap()
            val parser = SemanticParser()
            val analysis = parser.parseNodeTree(rootNode)
            rootNode.recycle()
            analysis.elementMap
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting interactive elements", e)
            emptyMap()
        }
    }
    
    /**
     * Execute a task using AI Agent
     */
    fun executeTask(task: String, callback: (AgentResult) -> Unit) {
        serviceScope.launch {
            val agent = Agent(this@OperitAccessibilityService)
            val result = agent.run(task)
            callback(result)
        }
    }
}
