package com.ryoobach.easynavistarter

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
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
                // 일반 앱 권한만으로 다른 앱을 강제 종료하거나 전원 메뉴를 띄울 수 없습니다.
                Log.i(TAG, "Power disconnected; no privileged action requested")
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val enabled = prefs.getBoolean(MainActivity.KEY_AUTO, true)
                val savedPackage = prefs.getString(MainActivity.KEY_PACKAGE, null)
                Log.i(TAG, "Boot completed; autoLaunch=$enabled; savedPackage=$savedPackage")
                if (enabled) {
                    // 부팅 직후 패키지/런처 초기화 시간을 준 뒤 기존 앱 작업 복귀 또는 신규 실행을 요청합니다.
                    val pending = goAsync()
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            launchNavigation(context)
                        } finally {
                            pending.finish()
                        }
                    }, BOOT_LAUNCH_DELAY_MS)
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

        // NEW_TASK: 해당 앱의 기존 task가 남아 있으면 앞으로 가져오고,
        // task가 없으면 새 task를 시작하도록 Android의 task 관리에 맡깁니다.
        // RESET_TASK_IF_NEEDED: 런처 실행 시 적용되는 task 복원/초기화 동작을 요청합니다.
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )

        try {
            if (Build.VERSION.SDK_INT >= 34) {
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
            Log.i(TAG, "Boot navigation task launch requested for saved package=$savedPackage; Android will reuse an eligible existing task or create one")
        } catch (e: Exception) {
            Log.e(TAG, "Boot navigation launch failed for saved package=$savedPackage", e)
        }
    }

    companion object {
        private const val TAG = "EasyNaviPower"
        private const val BOOT_NAV_REQUEST_CODE = 7401
        private const val BOOT_LAUNCH_DELAY_MS = 5_000L
    }
}
