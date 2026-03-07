package com.ai.assistance.operit.ui.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Semantic Parser - UI Element Parser
 * Parses raw UI hierarchy into structured data
 */
class SemanticParser {
    
    fun parseNodeTree(rootNode: AccessibilityNodeInfo): ScreenAnalysis {
        val interactiveNodes = mutableMapOf<Int, InteractiveElement>()
        val uiRepresentation = StringBuilder()
        
        parseRecursive(rootNode, interactiveNodes, uiRepresentation, 0, 0)
        
        return ScreenAnalysis(
            uiRepresentation = uiRepresentation.toString(),
            elementMap = interactiveNodes,
            keyboardOpen = false,
            activityName = rootNode.packageName?.toString() ?: "",
            pixelsAbove = 0,
            pixelsBelow = 0,
            screenWidth = 0,
            screenHeight = 0
        )
    }
    
    private fun parseRecursive(
        node: AccessibilityNodeInfo,
        interactiveNodes: MutableMap<Int, InteractiveElement>,
        uiRepresentation: StringBuilder,
        depth: Int,
        currentIndex: Int
    ): Int {
        if (!node.isVisibleToUser) return currentIndex
        
        val isImportant = isSemanticallyImportant(node)
        val isInteractive = isInteractive(node)
        
        var elementIndex = currentIndex
        
        if (isImportant || isInteractive) {
            val text = node.text?.toString() ?: ""
            val contentDescription = node.contentDescription?.toString() ?: ""
            val resourceId = node.resourceId?.toString() ?: ""
            val className = node.className?.toString() ?: ""
            
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            val extraInfo = mutableListOf<String>()
            if (node.isClickable) extraInfo.add("clickable")
            if (node.isLongClickable) extraInfo.add("long-clickable")
            if (node.isScrollable) extraInfo.add("scrollable")
            if (node.isEditable) extraInfo.add("editable")
            if (node.isCheckable) extraInfo.add("checkable")
            if (node.isChecked) extraInfo.add("checked")
            if (node.isEnabled) extraInfo.add("enabled")
            if (node.isFocused) extraInfo.add("focused")
            if (node.isSelected) extraInfo.add("selected")
            
            if (isInteractive) {
                val element = InteractiveElement(
                    index = elementIndex,
                    text = text,
                    contentDescription = contentDescription,
                    resourceId = resourceId,
                    className = className,
                    bounds = bounds,
                    states = extraInfo,
                    node = node
                )
                interactiveNodes[elementIndex] = element
                
                uiRepresentation.appendLine()
                uiRepresentation.appendLine("#### Element $elementIndex")
                uiRepresentation.appendLine("- **Text**: $text")
                uiRepresentation.appendLine("- **Resource ID**: $resourceId")
                uiRepresentation.appendLine("- **Class**: $className")
                uiRepresentation.appendLine("- **Content Description**: $contentDescription")
                uiRepresentation.appendLine("- **Bounds**: [$bounds.left, $bounds.top][$bounds.right, $bounds.bottom]")
                uiRepresentation.appendLine("- **States**: ${extraInfo.joinToString(", ")}")
                
                elementIndex++
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                elementIndex = parseRecursive(child, interactiveNodes, uiRepresentation, depth + 1, elementIndex)
                child.recycle()
            }
        }
        
        return elementIndex
    }
    
    private fun isSemanticallyImportant(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        val resourceId = node.resourceId?.toString()
        
        return (!text.isNullOrBlank() && text.length > 1) ||
               !contentDesc.isNullOrBlank() ||
               (!resourceId.isNullOrBlank() && resourceId != "null" && !resourceId.endsWith(":id/action_bar_root"))
    }
    
    private fun isInteractive(node: AccessibilityNodeInfo): Boolean {
        return node.isClickable ||
               node.isLongClickable ||
               node.isCheckable ||
               node.isScrollable ||
               node.isEditable ||
               (node.isFocusable && node.isEnabled)
    }
    
    fun findElementByIndex(elementMap: Map<Int, InteractiveElement>, index: Int): InteractiveElement? {
        return elementMap[index]
    }
    
    fun findElementByText(elementMap: Map<Int, InteractiveElement>, text: String): InteractiveElement? {
        return elementMap.values.find { it.text.equals(text, ignoreCase = true) }
    }
    
    fun findElementByResourceId(elementMap: Map<Int, InteractiveElement>, resourceId: String): InteractiveElement? {
        return elementMap.values.find { it.resourceId == resourceId }
    }
    
    fun findElementsByClassName(elementMap: Map<Int, InteractiveElement>, className: String): List<InteractiveElement> {
        return elementMap.values.filter { it.className.contains(className, ignoreCase = true) }
    }
    
    fun findClickableElements(elementMap: Map<Int, InteractiveElement>): List<InteractiveElement> {
        return elementMap.values.filter { it.states.contains("clickable") }
    }
    
    fun findEditableElements(elementMap: Map<Int, InteractiveElement>): List<InteractiveElement> {
        return elementMap.values.filter { it.states.contains("editable") }
    }
    
    fun findScrollableElements(elementMap: Map<Int, InteractiveElement>): List<InteractiveElement> {
        return elementMap.values.filter { it.states.contains("scrollable") }
    }
}

data class InteractiveElement(
    val index: Int,
    val text: String,
    val contentDescription: String,
    val resourceId: String,
    val className: String,
    val bounds: android.graphics.Rect,
    val states: List<String>,
    val node: AccessibilityNodeInfo? = null
)

data class ScreenAnalysis(
    val uiRepresentation: String,
    val elementMap: Map<Int, InteractiveElement>,
    val keyboardOpen: Boolean,
    val activityName: String,
    val pixelsAbove: Int,
    val pixelsBelow: Int,
    val screenWidth: Int,
    val screenHeight: Int
)
