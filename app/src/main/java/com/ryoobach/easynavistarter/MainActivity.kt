package com.ryoobach.easynavistarter

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var appSpinner: Spinner
    private lateinit var autoLaunch: CheckBox
    private lateinit var restoreOnBoot: CheckBox
    private lateinit var status: TextView
    private var launchableApps: List<AppChoice> = emptyList()

    data class AppChoice(val label: String, val packageName: String) {
        override fun toString(): String = label
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 28)
        }
        root.addView(TextView(this).apply { text = "이지 네비 스타터  0.1.0"; textSize = 22f })
        root.addView(TextView(this).apply {
            text = "차량 USB 전원 연결을 감지해 선택한 내비게이션을 실행하는 초기 테스트 버전입니다."
            textSize = 15f
            setPadding(0, 18, 0, 18)
        })
        root.addView(TextView(this).apply { text = "실행할 내비게이션 앱"; textSize = 16f })
        appSpinner = Spinner(this)
        root.addView(appSpinner)
        autoLaunch = CheckBox(this).apply {
            text = "차량 전원 연결 시 자동 실행 시도"
            isChecked = prefs.getBoolean(KEY_AUTO, false)
        }
        root.addView(autoLaunch)
        restoreOnBoot = CheckBox(this).apply {
            text = "휴대폰 재부팅 후 자동 실행 시도"
            isChecked = prefs.getBoolean(KEY_BOOT, false)
        }
        root.addView(restoreOnBoot)
        root.addView(Button(this).apply {
            text = "설정 저장"
            setOnClickListener { saveSettings(); showStatus("설정을 저장했습니다.") }
        })
        root.addView(Button(this).apply {
            text = "선택한 내비게이션 실행 테스트"
            setOnClickListener { saveSettings(); launchSelectedApp() }
        })
        root.addView(Button(this).apply {
            text = "설치된 앱 목록 새로고침"
            setOnClickListener { loadApps() }
        })
        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 20, 0, 0)
            gravity = Gravity.START
            text = "대기 중"
        }
        root.addView(status)
        root.addView(TextView(this).apply {
            text = "참고: Android 버전과 제조사 정책에 따라 백그라운드 자동 실행이 제한될 수 있습니다. 실제 차량 USB 환경에서 확인이 필요합니다."
            textSize = 12f
            setPadding(0, 20, 0, 0)
        })
        setContentView(root)
        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        launchableApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { info ->
                if (pm.getLaunchIntentForPackage(info.packageName) != null && info.packageName != packageName) {
                    AppChoice(pm.getApplicationLabel(info).toString(), info.packageName)
                } else null
            }.sortedBy { it.label.lowercase() }
        appSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, launchableApps)
        val saved = prefs.getString(KEY_PACKAGE, null)
        val index = launchableApps.indexOfFirst { it.packageName == saved }
        if (index >= 0) appSpinner.setSelection(index)
        showStatus("실행 가능한 앱 " + launchableApps.size + "개를 찾았습니다.")
    }

    private fun saveSettings() {
        val selected = launchableApps.getOrNull(appSpinner.selectedItemPosition)
        prefs.edit().putString(KEY_PACKAGE, selected?.packageName)
            .putBoolean(KEY_AUTO, autoLaunch.isChecked)
            .putBoolean(KEY_BOOT, restoreOnBoot.isChecked).apply()
    }

    private fun launchSelectedApp() {
        val selected = launchableApps.getOrNull(appSpinner.selectedItemPosition)
        if (selected == null) {
            showStatus("먼저 실행할 앱을 선택해 주세요.")
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(selected.packageName)
        if (intent == null) {
            showStatus("선택한 앱을 실행할 수 없습니다.")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
            showStatus(selected.label + " 실행을 요청했습니다.")
        } catch (e: Exception) {
            showStatus("앱 실행 실패: " + (e.message ?: "알 수 없는 오류"))
        }
    }

    private fun showStatus(message: String) {
        if (::status.isInitialized) status.text = message
    }

    companion object {
        const val PREFS = "easy_navi_settings"
        const val KEY_PACKAGE = "navigation_package"
        const val KEY_AUTO = "auto_launch"
        const val KEY_BOOT = "restore_on_boot"
    }
}
