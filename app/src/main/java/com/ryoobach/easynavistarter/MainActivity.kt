package com.ryoobach.easynavistarter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var appSpinner: Spinner
    private lateinit var autoLaunch: CheckBox
    private lateinit var autoShutdown: CheckBox
    private lateinit var status: TextView
    private var choices: List<AppChoice> = emptyList()
    private var settingSpinner = false

    data class AppChoice(val label: String, val packageName: String? = null, val addAction: Boolean = false) {
        override fun toString(): String = label
    }

    private val fixedApps = listOf(
        AppChoice("티맵", "com.skt.tmap"),
        AppChoice("카카오내비", "com.locnall.KimGiSa"),
        AppChoice("네이버지도", "com.nhn.android.nmap")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }
        page.addView(TextView(this).apply {
            text = "이지 네비 스타터  0.2.1"
            textSize = 23f
            setTextColor(0xFF17365D.toInt())
        })
        page.addView(TextView(this).apply {
            text = "차량 전원과 내비게이션 실행 설정"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(22))
        })

        addSectionTitle(page, "기기 시작 시")
        autoLaunch = CheckBox(this).apply {
            text = "내비게이션 자동 실행"
            isChecked = prefs.getBoolean(KEY_AUTO, true)
            textSize = 16f
        }
        page.addView(autoLaunch)

        addSectionTitle(page, "내비게이션 선택")
        appSpinner = Spinner(this)
        page.addView(appSpinner)

        addSectionTitle(page, "충전 종료 시")
        autoShutdown = CheckBox(this).apply {
            text = "내비게이션 및 휴대폰 자동 종료"
            isChecked = prefs.getBoolean(KEY_SHUTDOWN, false)
            textSize = 16f
        }
        page.addView(autoShutdown)

        page.addView(Button(this).apply {
            text = "저장"
            setOnClickListener {
                saveSettings()
                showStatus("설정을 저장했습니다.")
            }
        })

        page.addView(Button(this).apply {
            text = "선택한 내비게이션 실행 테스트"
            setOnClickListener {
                saveSettings()
                launchSelectedApp()
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
            text = "안내: Android 보안 정책상 일반 앱은 다른 앱을 강제 종료하거나 휴대폰 전원을 완전히 끌 수 없습니다. 충전 해제 감지와 백그라운드 자동 실행도 기기 및 Android 버전에 따라 제한될 수 있습니다."
            textSize = 12f
            setPadding(0, dp(18), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(page) })
        loadChoices()
    }

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(0xFF245B87.toInt())
            setPadding(0, dp(18), 0, dp(4))
        })
    }

    private fun loadChoices(selectedPackage: String? = prefs.getString(KEY_PACKAGE, null)) {
        val list = fixedApps.toMutableList()
        list.add(AppChoice("＋ 내비게이션 추가", addAction = true))
        choices = list
        settingSpinner = true
        appSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices)
        val index = choices.indexOfFirst { it.packageName == selectedPackage }
        appSpinner.setSelection(if (index >= 0) index else 0)
        appSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (settingSpinner) {
                    settingSpinner = false
                    return
                }
                if (choices.getOrNull(position)?.addAction == true) {
                    showInstalledAppPicker()
                }
            }
        }
    }

    private fun showInstalledAppPicker() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { info ->
                if (info.packageName != packageName && pm.getLaunchIntentForPackage(info.packageName) != null) {
                    AppChoice(pm.getApplicationLabel(info).toString(), info.packageName)
                } else null
            }
            .sortedBy { it.label.lowercase() }

        if (apps.isEmpty()) {
            showStatus("추가할 수 있는 앱을 찾지 못했습니다.")
            loadChoices()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("추가할 내비게이션 앱 선택")
            .setItems(apps.map { it.label }.toTypedArray()) { _, which ->
                val chosen = apps[which]
                prefs.edit().putString(KEY_PACKAGE, chosen.packageName).apply()
                loadChoices(chosen.packageName)
                showStatus(chosen.label + "을(를) 선택했습니다. 저장을 눌러 설정을 확정해 주세요.")
            }
            .setOnCancelListener { loadChoices() }
            .show()
    }

    private fun saveSettings() {
        val selected = choices.getOrNull(appSpinner.selectedItemPosition)
        val packageName = selected?.packageName ?: prefs.getString(KEY_PACKAGE, null)
        prefs.edit()
            .putString(KEY_PACKAGE, packageName)
            .putBoolean(KEY_AUTO, autoLaunch.isChecked)
            .putBoolean(KEY_SHUTDOWN, autoShutdown.isChecked)
            .putBoolean(KEY_RESTORE_LAST_LOCATION, true)
            .apply()
    }

    private fun launchSelectedApp() {
        val selected = choices.getOrNull(appSpinner.selectedItemPosition)
        val packageName = selected?.packageName ?: prefs.getString(KEY_PACKAGE, null)
        if (packageName.isNullOrBlank()) {
            showStatus("먼저 내비게이션 앱을 선택해 주세요.")
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            showStatus("선택한 앱이 설치되어 있지 않습니다. 앱을 설치하거나 다른 앱을 추가해 주세요.")
            return
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            showStatus("내비게이션 실행을 요청했습니다.")
        } catch (e: Exception) {
            showStatus("앱 실행 실패: " + (e.message ?: "알 수 없는 오류"))
        }
    }

    private fun showAppInfo() {
        AlertDialog.Builder(this)
            .setTitle("앱 정보")
            .setMessage("이지 네비 스타터\n버전 0.2.1\nAndroid\n\nRyoo jaesang & 최피디")
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
