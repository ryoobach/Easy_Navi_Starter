package com.ryoobach.easynavistarter

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class PowerMenuAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showPowerMenu() {
        // Move the navigation app out of the foreground, then show Android's system power dialog.
        val movedHome = performGlobalAction(GLOBAL_ACTION_HOME)
        Log.i(TAG, "HOME requested: $movedHome")
        handler.postDelayed({
            val shown = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            Log.i(TAG, "System power dialog requested: $shown")
        }, 700L)
    }

    companion object {
        private const val TAG = "EasyNaviPowerMenu"
        @Volatile private var instance: PowerMenuAccessibilityService? = null

        fun requestPowerMenu() {
            val service = instance
            if (service == null) {
                Log.w(TAG, "Accessibility service is not enabled; power menu cannot be opened")
                return
            }
            service.handler.post { service.showPowerMenu() }
        }
    }
}
