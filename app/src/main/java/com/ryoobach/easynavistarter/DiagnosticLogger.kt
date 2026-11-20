package com.ryoobach.easynavistarter

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {
    private const val FILE_NAME = "system_diagnostics.txt"
    private const val MAX_BYTES = 1024L * 1024L

    @Synchronized
    fun log(context: Context, event: String, detail: String = "") {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > MAX_BYTES) {
                val backup = File(context.filesDir, "system_diagnostics_previous.txt")
                if (backup.exists()) backup.delete()
                file.renameTo(backup)
            }
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.KOREA).format(Date())
            val suffix = if (detail.isBlank()) "" else " | $detail"
            file.appendText("[$timestamp] $event$suffix\n", Charsets.UTF_8)
        } catch (_: Exception) {
            // Diagnostics must never interrupt normal app behavior.
        }
    }

    fun readAll(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        val header = "Easy Navi Starter 시스템 진단\n앱 버전: ${appVersion(context)}\nAndroid: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n기기: ${Build.MANUFACTURER} ${Build.MODEL}\n----------------------------------------\n"
        val previous = File(context.filesDir, "system_diagnostics_previous.txt")
        return header + (if (previous.exists()) "[이전 기록 보관본]\n" + previous.readText(Charsets.UTF_8) else "") +
            (if (file.exists()) file.readText(Charsets.UTF_8) else "아직 기록이 없습니다.\n")
    }

    private fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }
}
