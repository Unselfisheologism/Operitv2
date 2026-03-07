package com.ai.assistance.operit.ui.automation

import android.content.Context

/**
 * Finger API - Gesture execution wrapper
 * Provides high-level methods for device interaction
 */
class Finger(private val context: Context) {
    
    private val service: ScreenInteractionService?
        get() = ScreenInteractionService.instance
    
    // ==================== Gesture Methods ====================
    
    fun tap(x: Int, y: Int): Boolean {
        return service?.clickOnPoint(x.toFloat(), y.toFloat()) ?: false
    }
    
    fun longPress(x: Int, y: Int): Boolean {
        return service?.longClickOnPoint(x.toFloat(), y.toFloat()) ?: false
    }
    
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int = 1000): Boolean {
        return service?.swipe(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), duration.toLong()) ?: false
    }
    
    fun type(text: String): Boolean {
        val result = service?.typeTextInFocusedField(text) ?: false
        if (result) {
            enter()
        }
        return result
    }
    
    fun enter(): Boolean {
        return service?.pressEnter() ?: false
    }
    
    // ==================== Global Actions ====================
    
    fun back(): Boolean = service?.performBack() ?: false
    fun home(): Boolean = service?.performHome() ?: false
    fun switchApp(): Boolean = service?.performRecents() ?: false
    fun notifications(): Boolean = service?.expandNotifications() ?: false
    fun powerMenu(): Boolean = service?.openPowerMenu() ?: false
    
    // ==================== Scroll Methods ====================
    
    fun scrollUp(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenHeight = getScreenHeight()
        val screenWidth = getScreenWidth()
        return swipe(
            screenWidth / 2,
            screenHeight / 2 + pixels / 2,
            screenWidth / 2,
            screenHeight / 2 - pixels / 2,
            duration
        )
    }
    
    fun scrollDown(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenHeight = getScreenHeight()
        val screenWidth = getScreenWidth()
        return swipe(
            screenWidth / 2,
            screenHeight / 2 - pixels / 2,
            screenWidth / 2,
            screenHeight / 2 + pixels / 2,
            duration
        )
    }
    
    fun scrollLeft(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenHeight = getScreenHeight()
        val screenWidth = getScreenWidth()
        return swipe(
            screenWidth / 2 + pixels / 2,
            screenHeight / 2,
            screenWidth / 2 - pixels / 2,
            screenHeight / 2,
            duration
        )
    }
    
    fun scrollRight(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenHeight = getScreenHeight()
        val screenWidth = getScreenWidth()
        return swipe(
            screenWidth / 2 - pixels / 2,
            screenHeight / 2,
            screenWidth / 2 + pixels / 2,
            screenHeight / 2,
            duration
        )
    }
    
    fun scrollDownPrecisely(pixels: Int, pixelsPerSecond: Int = 1000): Boolean {
        return service?.scrollDownPrecisely(pixels, pixelsPerSecond) ?: false
    }
    
    fun scrollUpPrecisely(pixels: Int, pixelsPerSecond: Int = 1000): Boolean {
        return service?.scrollUpPrecisely(pixels, pixelsPerSecond) ?: false
    }
    
    // ==================== Utility Methods ====================
    
    private fun getScreenWidth(): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels
    }
    
    private fun getScreenHeight(): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels
    }
}

fun Context.getFinger(): Finger = Finger(this)
