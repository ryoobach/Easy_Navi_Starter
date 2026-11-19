package com.ryoobach.easynavistarter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class PowerEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.i(TAG, "Power connected")
                notificationManager(context).cancel(NOTIFICATION_ID)
                if (prefs.getBoolean(MainActivity.KEY_AUTO, true)) launchNavigation(context, "전원 연결")
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.i(TAG, "Power disconnected")
                if (prefs.getBoolean(MainActivity.KEY_SHUTDOWN, false)) showShutdownPrompt(context)
            }
            ACTION_CONFIRM_SHUTDOWN -> {
                notificationManager(context).cancel(NOTIFICATION_ID)
                Log.i(TAG, "Shutdown confirmed by user")
                PowerMenuAccessibilityService.requestPowerMenu()
            }
            ACTION_CANCEL_SHUTDOWN -> {
                notificationManager(context).cancel(NOTIFICATION_ID)
                Log.i(TAG, "Shutdown cancelled by user")
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Boot completed")
                if (prefs.getBoolean(MainActivity.KEY_AUTO, true)) launchNavigation(context, "기기 시작")
            }
        }
    }

    private fun showShutdownPrompt(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission is not granted; cannot show unplug prompt")
            return
        }
        val manager = notificationManager(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "충전 분리 확인", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "충전 케이블 분리 시 내비게이션 및 기기 종료 여부를 확인합니다."
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
        val yesIntent = PendingIntent.getBroadcast(
            context, 1, Intent(context, PowerEventReceiver::class.java).setAction(ACTION_CONFIRM_SHUTDOWN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val noIntent = PendingIntent.getBroadcast(
            context, 2, Intent(context, PowerEventReceiver::class.java).setAction(ACTION_CANCEL_SHUTDOWN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            context, 3, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_MAX)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setContentTitle("충전이 중단되었습니다")
            .setContentText("내비게이션을 종료하고 기기를 종료하시겠습니까?")
            .setStyle(Notification.BigTextStyle().bigText("충전이 중단되었습니다.\n내비게이션을 종료하고 기기를 종료하시겠습니까?"))
            .setCategory(Notification.CATEGORY_SYSTEM)
            .setPriority(Notification.PRIORITY_MAX)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_lock_power_off, "예", yesIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "아니요", noIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "Unplug confirmation notification shown")
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        private const val CHANNEL_ID = "power_disconnect_confirmation"
        private const val NOTIFICATION_ID = 7301
        private const val ACTION_CONFIRM_SHUTDOWN = "com.ryoobach.easynavistarter.CONFIRM_SHUTDOWN"
        private const val ACTION_CANCEL_SHUTDOWN = "com.ryoobach.easynavistarter.CANCEL_SHUTDOWN"
    }
}
