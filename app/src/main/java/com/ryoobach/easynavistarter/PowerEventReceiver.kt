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
                val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                DiagnosticLogger.log(context, "충전 케이블 연결 이벤트 수신", "배터리 상태=${battery?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1}")
                Log.i(TAG, "Power connected: no navigation action")
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                DiagnosticLogger.log(context, "충전 케이블 분리 이벤트 수신", "배터리 상태=${battery?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1}; 앱 정책상 내비 종료/기기 전원 종료는 실행하지 않음")
                Log.i(TAG, "Power disconnected; no privileged action requested")
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                DiagnosticLogger.log(context, "기기 부팅 완료 이벤트 수신")
                val enabled = prefs.getBoolean(MainActivity.KEY_AUTO, true)
                val savedPackage = prefs.getString(MainActivity.KEY_PACKAGE, null)
                prefs.edit()
                    .putLong(MainActivity.KEY_DIAG_BOOT_TIME, System.currentTimeMillis())
                    .putString(MainActivity.KEY_DIAG_BOOT_PACKAGE, savedPackage ?: "")
                    .putString(
                        MainActivity.KEY_DIAG_RESULT,
                        if (enabled) "부팅 이벤트 수신됨. 5초 후 실행 요청 예정."
                        else "부팅 이벤트 수신됨. 자동 실행 설정이 꺼져 있음."
                    )
                    .putLong(MainActivity.KEY_DIAG_RESULT_TIME, System.currentTimeMillis())
                    .commit()
                Log.i(TAG, "Boot completed; autoLaunch=$enabled; savedPackage=$savedPackage")
                if (enabled) {
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
        DiagnosticLogger.log(context, "내비게이션 실행 요청 시작", "저장 패키지=${savedPackage ?: "없음"}")
        prefs.edit()
            .putLong(MainActivity.KEY_DIAG_ATTEMPT_TIME, System.currentTimeMillis())
            .putString(MainActivity.KEY_DIAG_RESULT, "내비게이션 실행 요청 준비 중.")
            .commit()

        if (savedPackage.isNullOrBlank()) {
            DiagnosticLogger.log(context, "내비게이션 실행 실패", "저장된 내비게이션 앱이 없음")
            recordResult(context, "실행 실패: 저장된 내비게이션 앱이 없습니다.")
            return
        }

        val candidates = if (savedPackage == "com.skt.tmap" || savedPackage == "com.skt.tmap.ku") {
            listOf(savedPackage, "com.skt.tmap.ku", "com.skt.tmap").distinct()
        } else listOf(savedPackage)

        val resolvedPackage = candidates.firstOrNull {
            context.packageManager.getLaunchIntentForPackage(it) != null
        }
        val launchIntent = resolvedPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (launchIntent == null) {
            DiagnosticLogger.log(context, "내비게이션 실행 실패", "실행 화면을 찾지 못함; 저장 패키지=$savedPackage")
            recordResult(context, "실행 실패: 앱 실행 화면을 찾지 못했습니다. 저장 패키지=$savedPackage")
            Log.w(TAG, "No launchable activity for saved package=$savedPackage")
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                val creatorOptions = ActivityOptions.makeBasic().apply {
                    setPendingIntentCreatorBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, BOOT_NAV_REQUEST_CODE, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    creatorOptions.toBundle()
                )
                val senderOptions = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                pendingIntent.send(context, 0, null, null, null, null, senderOptions.toBundle())
            } else {
                PendingIntent.getActivity(
                    context, BOOT_NAV_REQUEST_CODE, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ).send()
            }
            DiagnosticLogger.log(context, "내비게이션 실행 요청 전송됨", "대상 패키지=$resolvedPackage; 실제 화면 표시 여부는 별도 확인 필요")
            recordResult(
                context,
                "실행 요청 전송됨: $resolvedPackage. Android가 실제 화면 표시를 허용했는지는 별도 확인이 필요합니다."
            )
            Log.i(TAG, "Boot navigation task launch requested for package=$resolvedPackage")
        } catch (e: Exception) {
            val detail = e.message ?: e.javaClass.simpleName
            DiagnosticLogger.log(context, "내비게이션 실행 요청 오류", detail)
            recordResult(context, "실행 요청 오류: $detail")
            Log.e(TAG, "Boot navigation launch failed for package=$savedPackage", e)
        }
    }

    private fun recordResult(context: Context, result: String) {
        context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).edit()
            .putString(MainActivity.KEY_DIAG_RESULT, result)
            .putLong(MainActivity.KEY_DIAG_RESULT_TIME, System.currentTimeMillis())
            .commit()
    }

    companion object {
        private const val TAG = "EasyNaviPower"
        private const val BOOT_NAV_REQUEST_CODE = 7401
        private const val BOOT_LAUNCH_DELAY_MS = 5_000L
    }
}
