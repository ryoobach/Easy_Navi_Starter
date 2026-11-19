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
    private lateinit var autoLaunch: CheckBox
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
            text = "Easy Navi Starter  0.2.5"
            textSize = 23f
            setTextColor(0xFF17365D.toInt())
        })
        page.addView(TextView(this).apply {
            text = "간편한 내비게이션 자동 실행"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(18))
        })

        addSectionTitle(page, "기기 시작 시")
        autoLaunch = CheckBox(this).apply {
            text = "내비게이션 자동 실행"
            textSize = 16f
            isChecked = prefs.getBoolean(KEY_AUTO, true)
        }
        page.addView(autoLaunch)

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
            text = "충전 종료 시 휴대폰 자동 종료"
            isChecked = prefs.getBoolean(KEY_SHUTDOWN, false)
            textSize = 16f
        }
        page.addView(autoShutdown)

        page.addView(Button(this).apply {
            text = "설정 저장"
            setOnClickListener {
                if (saveSettings()) showStatus("설정을 저장했습니다.")
            }
        })

        status = TextView(this).apply {
            text = "설정 대기 중"
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        page.addView(status)

        page.addView(Button(this).apply {
            text = "앱 정보"
            setOnClickListener { showAppInfo() }
        })

        page.addView(TextView(this).apply {
            text = "부팅 후 자동 실행 · 광고 없음"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, dp(12), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(page) })
        restoreSettings()
    }

    private fun restoreSettings() {
        val savedPackage = prefs.getString(KEY_PACKAGE, fixedApps.first().packageName)
        val selectedIndex = fixedApps.indexOfFirst { it.packageName == savedPackage }
            .let { if (it >= 0) it else 0 }
        navigationGroup.check(selectedIndex + 1)
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
            .putBoolean(KEY_AUTO, autoLaunch.isChecked)
            .putBoolean(KEY_SHUTDOWN, autoShutdown.isChecked)
            .putBoolean(KEY_RESTORE_LAST_LOCATION, true)
            .apply()
        return true
    }

    private fun showAppInfo() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.2.5"
        } catch (_: Exception) { "0.2.5" }
        AlertDialog.Builder(this)
            .setTitle("앱 정보")
            .setMessage("Easy Navi Starter\n버전 $version\nAndroid\n\nRyoo jaesang & 최피디")
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
