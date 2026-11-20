package com.ryoobach.easynavistarter

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class PowerEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                // 케이블 연결은 내비 자동 실행 조건이 아닙니다.
                Log.i(TAG, "Power connected: no navigation action")
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.i(TAG, "Power disconnected")
                if (prefs.getBoolean(MainActivity.KEY_SHUTDOWN, false)) {
                    PowerMenuAccessibilityService.requestCloseNavigationAndShowPowerMenu()
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Boot completed; autoLaunch=${prefs.getBoolean(MainActivity.KEY_AUTO, true)}")
                if (prefs.getBoolean(MainActivity.KEY_AUTO, true)) {
                    launchNavigation(context)
                }
            }
        }
    }

    private fun launchNavigation(context: Context) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val savedPackage = prefs.getString(MainActivity.KEY_PACKAGE, null)
        if (savedPackage.isNullOrBlank()) {
            Log.w(TAG, "Boot launch skipped: no saved navigation package")
            return
        }

        // 티맵의 구형/현재 패키지명을 모두 확인합니다.
        val candidates = if (savedPackage == "com.skt.tmap" || savedPackage == "com.skt.tmap.ku") {
            listOf(savedPackage, "com.skt.tmap.ku", "com.skt.tmap").distinct()
        } else {
            listOf(savedPackage)
        }
        val launchIntent = candidates.firstNotNullOfOrNull {
            context.packageManager.getLaunchIntentForPackage(it)
        }
        if (launchIntent == null) {
            Log.w(TAG, "Boot launch skipped: no launchable activity for saved package=$savedPackage")
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+ requires explicit background-activity-launch opt-in
                // on both the PendingIntent creator and sender sides.
                val creatorOptions = ActivityOptions.makeBasic().apply {
                    setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    BOOT_NAV_REQUEST_CODE,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    creatorOptions.toBundle()
                )
                val senderOptions = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                pendingIntent.send(
                    context, 0, null, null, null, null, senderOptions.toBundle()
                )
            } else {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    BOOT_NAV_REQUEST_CODE,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent.send()
            }
            Log.i(TAG, "Boot navigation PendingIntent sent for saved package=$savedPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Boot navigation launch failed for saved package=$savedPackage", e)
        }
    }

    companion object {
        private const val TAG = "EasyNaviPower"
        private const val BOOT_NAV_REQUEST_CODE = 7401
    }
}
