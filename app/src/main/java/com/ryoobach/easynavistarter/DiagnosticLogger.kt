package com.ryoobach.easynavistarter

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {
    private const val DIAGNOSTIC_FILE = "system_diagnostics.txt"
    private const val EVENT_FILE = "event_log.txt"
    private const val MAX_BYTES = 1024L * 1024L

    /**
     * 모든 기록은 기존 진단 로그와 별도의 이벤트 로그 양쪽에 남깁니다.
     * 이벤트 로그는 앱 내부 저장소에 자동 누적되며, 진단 TXT 내보내기에 함께 포함됩니다.
     */
    @Synchronized
    fun log(context: Context, event: String, detail: String = "") {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.KOREA).format(Date())
            val suffix = if (detail.isBlank()) "" else " | $detail"
            val line = "[$timestamp] $event$suffix\n"
            appendRotating(File(context.filesDir, DIAGNOSTIC_FILE), "system_diagnostics_previous.txt", line)
            appendRotating(File(context.filesDir, EVENT_FILE), "event_log_previous.txt", line)
        } catch (_: Exception) {
            // Diagnostics must never interrupt normal app behavior.
        }
    }

    private fun appendRotating(file: File, backupName: String, line: String) {
        if (file.exists() && file.length() + line.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
            val backup = File(file.parentFile, backupName)
            if (backup.exists()) backup.delete()
            file.renameTo(backup)
        }
        file.appendText(line, Charsets.UTF_8)
    }

    fun readAll(context: Context): String {
        val header = "Easy Navi Starter 시스템 진단\n앱 버전: ${appVersion(context)}\nAndroid: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n기기: ${Build.MANUFACTURER} ${Build.MODEL}\n----------------------------------------\n"
        val diagnosticPrevious = File(context.filesDir, "system_diagnostics_previous.txt")
        val diagnosticCurrent = File(context.filesDir, DIAGNOSTIC_FILE)
        val eventPrevious = File(context.filesDir, "event_log_previous.txt")
        val eventCurrent = File(context.filesDir, EVENT_FILE)
        return buildString {
            append(header)
            append("[진단 로그]\n")
            if (diagnosticPrevious.exists()) append("[이전 기록 보관본]\n").append(diagnosticPrevious.readText(Charsets.UTF_8))
            if (diagnosticCurrent.exists()) append(diagnosticCurrent.readText(Charsets.UTF_8))
            else append("아직 기록이 없습니다.\n")
            append("\n----------------------------------------\n[별도 이벤트 로그: event_log.txt]\n")
            if (eventPrevious.exists()) append("[이전 이벤트 기록 보관본]\n").append(eventPrevious.readText(Charsets.UTF_8))
            if (eventCurrent.exists()) append(eventCurrent.readText(Charsets.UTF_8))
            else append("아직 이벤트 기록이 없습니다.\n")
        }
    }

    /** 전체 이벤트 로그를 화면에서 확인하기 위한 문자열입니다. */
    @Synchronized
    fun readEventLog(context: Context): String {
        val previous = File(context.filesDir, "event_log_previous.txt")
        val current = File(context.filesDir, EVENT_FILE)
        return buildString {
            if (previous.exists()) append(previous.readText(Charsets.UTF_8))
            if (current.exists()) append(current.readText(Charsets.UTF_8))
        }
    }

    /** 공유용으로 가장 최근 N줄만 반환합니다. */
    @Synchronized
    fun readRecentEventLines(context: Context, maxLines: Int = 100): String {
        val lines = readEventLog(context).lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(maxLines.coerceAtLeast(1))
        return buildString {
            append("Easy Navi Starter 최근 진단 로그\n")
            append("앱 버전: ").append(appVersion(context)).append("\n")
            append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            append("최근 이벤트: ").append(lines.size).append("줄 (최대 100줄)\n")
            append("----------------------------------------\n")
            lines.forEach { append(it).append('\n') }
        }
    }

    private fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }
}
