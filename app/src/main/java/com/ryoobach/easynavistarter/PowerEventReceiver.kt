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
                    closeNavigationAndTryPowerOff(context)
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

    private fun closeNavigationAndTryPowerOff(context: Context) {
        // 일반 앱은 다른 앱의 프로세스를 강제 종료할 수 없습니다.
        // 홈 화면을 띄워 선택된 내비게이션을 전면에서 내립니다.
        try {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(home)
            Log.i(TAG, "Home activity requested to leave navigation")
        } catch (e: Exception) {
            Log.w(TAG, "Could not bring home screen to foreground", e)
        }

        // 완전한 전원 끄기는 Android 시스템/기기 소유자 권한이 필요합니다.
        // 루팅된 기기에서만 su 명령이 승인될 수 있습니다.
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot -p"))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Log.i(TAG, "Power-off command accepted by rooted device")
            } else {
                Log.w(TAG, "Power-off unavailable: root command denied (exit=$exitCode)")
            }
        } catch (e: Exception) {
            Log.i(TAG, "Full device shutdown unavailable to an ordinary app", e)
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
