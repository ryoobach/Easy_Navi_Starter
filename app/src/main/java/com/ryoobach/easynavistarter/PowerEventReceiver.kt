package com.ryoobach.easynavistarter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PowerEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                // 케이블 연결은 내비 자동 실행 조건이 아닙니다.
                // 내비 자동 실행은 오직 BOOT_COMPLETED에서 처리합니다.
                Log.i(TAG, "Power connected: no navigation action")
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.i(TAG, "Power disconnected")
                if (prefs.getBoolean(MainActivity.KEY_SHUTDOWN, false)) {
                    PowerMenuAccessibilityService.requestCloseNavigationAndShowPowerMenu()
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Boot completed")
                if (prefs.getBoolean(MainActivity.KEY_AUTO, true)) {
                    launchNavigation(context)
                }
            }
        }
    }

    private fun launchNavigation(context: Context) {
        val packageName = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .getString(MainActivity.KEY_PACKAGE, null) ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launchIntent)
            Log.i(TAG, "Boot navigation launch requested: $packageName")
        } catch (e: Exception) {
            Log.w(TAG, "Background launch blocked for $packageName", e)
        }
    }

    companion object {
        private const val TAG = "EasyNaviPower"
    }
}
