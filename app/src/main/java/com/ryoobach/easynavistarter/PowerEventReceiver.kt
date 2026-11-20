package com.ryoobach.easynavistarter

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class PowerEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: "null"
        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val eventDetail = "action=$action; 이벤트 기준 전원 연결=예; 배터리 상태는 1초 후 재확인"
                DiagnosticLogger.log(context, "충전 전원 연결 이벤트 감지", eventDetail)
                Log.i(TAG, eventDetail)
                verifyPowerStateAfterDelay(context, action, "연결")
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val eventDetail = "action=$action; 이벤트 기준 전원 연결=아니요; 배터리 상태는 1초 후 재확인"
                DiagnosticLogger.log(context, "충전 전원 분리 이벤트 감지", eventDetail)
                Log.i(TAG, eventDetail)
                verifyPowerStateAfterDelay(context, action, "분리")
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val batteryDetail = readBatteryDetail(context)
                DiagnosticLogger.log(context, "시스템 브로드캐스트 수신", "action=$action; $batteryDetail")
                Log.i(TAG, "Broadcast received: action=$action; $batteryDetail")
                DiagnosticLogger.log(context, "기기 부팅 완료 이벤트 수신")
                val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
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
            else -> {
                val batteryDetail = readBatteryDetail(context)
                DiagnosticLogger.log(context, "시스템 브로드캐스트 수신", "action=$action; $batteryDetail")
                Log.i(TAG, "Broadcast received: action=$action; $batteryDetail")
            }
        }
    }

    /**
     * POWER_CONNECTED / POWER_DISCONNECTED 이벤트와 BATTERY_CHANGED 스냅샷은
     * 시스템 내부 갱신 순서 때문에 수신 순간 서로 다른 상태를 보일 수 있습니다.
     * 이벤트 종류는 원본 브로드캐스트를 기준으로 기록하고, 실제 배터리 스냅샷은
     * 1초 뒤 다시 조회해 별도 항목으로 기록합니다.
     */
    private fun verifyPowerStateAfterDelay(context: Context, action: String, eventLabel: String) {
        val pending = goAsync()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val batteryDetail = readBatteryDetail(context)
                DiagnosticLogger.log(
                    context,
                    "충전 상태 지연 재확인",
                    "원본 이벤트=$action; 이벤트 기준=$eventLabel; $batteryDetail"
                )
                Log.i(TAG, "Delayed power-state verification: action=$action; $batteryDetail")
            } catch (e: Exception) {
                DiagnosticLogger.log(
                    context,
                    "충전 상태 재확인 오류",
                    "${e.javaClass.simpleName}: ${e.message ?: "no message"}"
                )
            } finally {
                pending.finish()
            }
        }, POWER_STATE_VERIFY_DELAY_MS)
    }

    private fun readBatteryDetail(context: Context): String {
        val battery = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            DiagnosticLogger.log(
                context,
                "배터리 상태 조회 오류",
                "${e.javaClass.simpleName}: ${e.message ?: "no message"}"
            )
            null
        }
        return battery?.let {
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val statusText = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "충전 중"
                BatteryManager.BATTERY_STATUS_FULL -> "완충"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "방전 중"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "충전 안 함"
                else -> "알 수 없음($status)"
            }
            val plugText = when {
                (plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> "USB"
                (plugged and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> "AC"
                (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> "무선"
                plugged == 0 -> "연결 없음"
                else -> "기타($plugged)"
            }
            "배터리 상태=$statusText; 전원 연결=${if (plugged != 0) "예" else "아니요"}; 연결 유형=$plugText; 배터리=$percent%; plugged=$plugged"
        } ?: "배터리 스냅샷 조회 불가"
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
                    setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, BOOT_NAV_REQUEST_CODE, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    creatorOptions.toBundle()
                )
                val senderOptions = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
                pendingIntent.send(context, 0, null, null, null, null, senderOptions.toBundle())
            } else {
                PendingIntent.getActivity(
                    context, BOOT_NAV_REQUEST_CODE, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ).send()
            }
            DiagnosticLogger.log(context, "내비게이션 실행 요청 전송됨", "대상 패키지=$resolvedPackage; 실제 화면 표시 여부는 별도 확인 필요")
            recordResult(context, "실행 요청 전송됨: $resolvedPackage. Android가 실제 화면 표시를 허용했는지는 별도 확인이 필요합니다.")
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
        private const val POWER_STATE_VERIFY_DELAY_MS = 1_000L
    }
}
