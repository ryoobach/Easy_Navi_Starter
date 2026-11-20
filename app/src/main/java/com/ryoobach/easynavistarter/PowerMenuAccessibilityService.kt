package com.ryoobach.easynavistarter

import android.accessibilityservice.AccessibilityService
import android.os.Build
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

    private fun closeNavigationAndLockScreen() {
        // 일반 앱은 다른 앱을 강제 종료할 수 없으므로 내비 화면에서 홈으로 이동합니다.
        val movedHome = performGlobalAction(GLOBAL_ACTION_HOME)
        Log.i(TAG, "HOME requested after power disconnect: $movedHome")

        handler.postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val locked = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.i(TAG, "Screen lock requested: $locked")
            } else {
                // Android 8/8.1에는 화면 잠금 접근성 액션이 없어 시스템 전원 메뉴를 표시합니다.
                val shown = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                Log.i(TAG, "Power dialog fallback requested: $shown")
            }
        }, 700L)
    }

    companion object {
        private const val TAG = "EasyNaviPowerMenu"
        @Volatile private var instance: PowerMenuAccessibilityService? = null

        fun requestCloseNavigationAndLockScreen() {
            val service = instance
            if (service == null) {
                Log.w(TAG, "Accessibility service is not enabled; cannot leave navigation or lock screen")
                return
            }
            service.handler.post { service.closeNavigationAndLockScreen() }
        }
    }
}
