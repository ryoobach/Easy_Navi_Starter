package com.ryoobach.easynavistarter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var navigationGroup: RadioGroup
    private lateinit var autoShutdown: CheckBox
    private lateinit var status: TextView

    data class AppChoice(val label: String, val packageName: String)

    private val fixedApps = listOf(
        AppChoice("티맵", "com.skt.tmap"),
        AppChoice("카카오맵", "net.daum.android.map"),
        AppChoice("네이버 지도", "com.nhn.android.nmap")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }
        page.addView(TextView(this).apply {
            text = "이지 네비 스타터  0.2.2"
            textSize = 23f
            setTextColor(0xFF17365D.toInt())
        })
        page.addView(TextView(this).apply {
            text = "차량 전원과 내비게이션 실행 설정"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(22))
        })

        addSectionTitle(page, "기기 시작 시")
        page.addView(TextView(this).apply {
            text = "기기가 시작되면 저장된 내비게이션을 자동으로 실행합니다."
            textSize = 14f
            setPadding(0, dp(4), 0, dp(8))
        })

        addSectionTitle(page, "내비게이션 선택")
        navigationGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        fixedApps.forEachIndexed { index, app ->
            navigationGroup.addView(RadioButton(this).apply {
                id = index + 1
                text = app.label
                textSize = 16f
                setPadding(0, dp(5), 0, dp(5))
            })
        }
        page.addView(navigationGroup)

        addSectionTitle(page, "충전 종료 시")
        autoShutdown = CheckBox(this).apply {
            text = "충전 종료 시 휴대폰 자동종료"
            isChecked = prefs.getBoolean(KEY_SHUTDOWN, false)
            textSize = 16f
        }
        page.addView(autoShutdown)

        page.addView(Button(this).apply {
            text = "저장"
            setOnClickListener {
                if (saveSettings()) showStatus("설정을 저장했습니다.")
            }
        })
        page.addView(Button(this).apply {
            text = "선택한 내비게이션 실행 테스트"
            setOnClickListener {
                if (saveSettings()) launchSelectedApp()
            }
        })
        page.addView(Button(this).apply {
            text = "앱 정보"
            setOnClickListener { showAppInfo() }
        })

        status = TextView(this).apply {
            text = "설정 대기 중"
            textSize = 13f
            setPadding(0, dp(16), 0, 0)
        }
        page.addView(status)
        page.addView(TextView(this).apply {
            text = "안내: Android 보안 정책상 일반 앱은 다른 앱을 강제 종료하거나 휴대폰 전원을 완전히 끌 수 없습니다. 충전 해제 감지와 백그라운드 자동 실행은 기기 및 Android 버전에 따라 제한될 수 있습니다."
            textSize = 12f
            setPadding(0, dp(18), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(page) })
        restoreSettings()
    }

    private fun restoreSettings() {
        val savedPackage = prefs.getString(KEY_PACKAGE, fixedApps.first().packageName)
        val selectedIndex = fixedApps.indexOfFirst { it.packageName == savedPackage }
            .let { if (it >= 0) it else 0 }
        navigationGroup.check(selectedIndex + 1)
        // 자동 실행은 기본 동작이며 별도의 켜기/끄기 체크 항목을 두지 않습니다.
        prefs.edit().putBoolean(KEY_AUTO, true).apply()
    }

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(0xFF245B87.toInt())
            setPadding(0, dp(18), 0, dp(4))
        })
    }

    private fun selectedApp(): AppChoice? {
        val index = navigationGroup.checkedRadioButtonId - 1
        return fixedApps.getOrNull(index)
    }

    private fun saveSettings(): Boolean {
        val selected = selectedApp()
        if (selected == null) {
            showStatus("내비게이션을 선택해 주세요.")
            return false
        }
        prefs.edit()
            .putString(KEY_PACKAGE, selected.packageName)
            .putBoolean(KEY_AUTO, true)
            .putBoolean(KEY_SHUTDOWN, autoShutdown.isChecked)
            .putBoolean(KEY_RESTORE_LAST_LOCATION, true)
            .apply()
        return true
    }

    private fun launchSelectedApp() {
        val selected = selectedApp() ?: return
        val intent = packageManager.getLaunchIntentForPackage(selected.packageName)
        if (intent == null) {
            showStatus("${selected.label} 앱이 설치되어 있지 않습니다.")
            return
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            showStatus("${selected.label} 실행을 요청했습니다.")
        } catch (e: Exception) {
            showStatus("앱 실행 실패: " + (e.message ?: "알 수 없는 오류"))
        }
    }

    private fun showAppInfo() {
        AlertDialog.Builder(this)
            .setTitle("앱 정보")
            .setMessage("이지 네비 스타터\n버전 0.2.2\nAndroid\n\nRyoo jaesang & 최피디")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun showStatus(message: String) {
        status.text = message
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "easy_navi_settings"
        const val KEY_PACKAGE = "navigation_package"
        const val KEY_AUTO = "auto_launch"
        const val KEY_SHUTDOWN = "auto_shutdown_on_unplug"
        const val KEY_RESTORE_LAST_LOCATION = "restore_last_location"
        const val KEY_BOOT = "restore_on_boot"
    }
}
