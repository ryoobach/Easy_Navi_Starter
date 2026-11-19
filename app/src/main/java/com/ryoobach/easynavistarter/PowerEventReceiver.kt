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
                Log.i(TAG, "Power connected")
                if (prefs.getBoolean(MainActivity.KEY_AUTO, true)) {
                    launchNavigation(context, "전원 연결")
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.i(TAG, "Power disconnected")
                if (prefs.getBoolean(MainActivity.KEY_SHUTDOWN, false)) {
                    PowerMenuAccessibilityService.requestPowerMenu()
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Boot completed")
                if (prefs.getBoolean(MainActivity.KEY_AUTO, true)) {
                    launchNavigation(context, "기기 시작")
                }
            }
        }
    }

    private fun launchNavigation(context: Context, reason: String) {
        val packageName = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
            .getString(MainActivity.KEY_PACKAGE, null) ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launchIntent)
            Log.i(TAG, "Launch requested: $packageName ($reason)")
        } catch (e: Exception) {
            Log.w(TAG, "Background launch blocked: $packageName ($reason)", e)
        }
    }

    companion object {
        private const val TAG = "EasyNaviPower"
    }
}
