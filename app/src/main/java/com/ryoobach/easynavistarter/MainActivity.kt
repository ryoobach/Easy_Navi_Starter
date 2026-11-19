package com.ryoobach.easynavistarter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var autoLaunch: CheckBox
    private lateinit var autoShutdown: CheckBox
    private lateinit var status: TextView
    private lateinit var navButtons: List<RadioButton>
    private var selectedPackage = "com.skt.tmap"
    private var selectedLabel = "티맵"

    private data class NavApp(val label: String, val packageName: String)

    private val builtInApps = listOf(
        NavApp("티맵", "com.skt.tmap"),
        NavApp("카카오맵", "net.daum.android.map"),
        NavApp("네이버 지도", "com.nhn.android.nmap")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        selectedPackage = prefs.getString(KEY_PACKAGE, builtInApps[0].packageName) ?: builtInApps[0].packageName
        selectedLabel = prefs.getString(KEY_LABEL, builtInApps.firstOrNull { it.packageName == selectedPackage }?.label ?: "추가한 내비") ?: "추가한 내비"

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(250, 250, 250))
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(2), dp(8))
        }
        val logo = ImageView(this).apply {
            setImageResource(com.ryoobach.easynavistarter.R.drawable.ic_launcher)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(66), dp(66)))
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        heading.addView(TextView(this).apply {
            text = "Easy Navi Starter"
            textSize = 23f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        })
        heading.addView(TextView(this).apply {
            text = "간편한 내비게이션 자동 실행"
            textSize = 15f
            setTextColor(Color.GRAY)
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(heading, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            text = "ⓘ"
            textSize = 27f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            contentDescription = "앱 정보"
            setOnClickListener { showAppInfo() }
        }, LinearLayout.LayoutParams(dp(42), dp(48)))
        page.addView(header)

        val bootCard = card()
        autoLaunch = CheckBox(this).apply {
            text = "부팅시 내비 자동실행"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
            isChecked = prefs.getBoolean(KEY_AUTO, true)
        }
        bootCard.addView(autoLaunch)
        bootCard.addView(description("스마트 기기를 켜면 부팅이 되면서 아래의\n내비가 자동 실행됩니다."))
        page.addView(bootCard)

        page.addView(sectionTitle("내비게이션 선택"))
        val navCard = card()
        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val secondRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val tmap = navRadio("티맵")
        val naver = navRadio("네이버 지도")
        val kakao = navRadio("카카오맵")
        val add = navRadio("네비 추가")
        navButtons = listOf(tmap, naver, kakao, add)
        listOf(firstRow, secondRow).forEach { row ->
            navCard.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)))
        }
        firstRow.addView(tmap, LinearLayout.LayoutParams(0, dp(54), 1f))
        firstRow.addView(naver, LinearLayout.LayoutParams(0, dp(54), 1f))
        secondRow.addView(kakao, LinearLayout.LayoutParams(0, dp(54), 1f))
        secondRow.addView(add, LinearLayout.LayoutParams(0, dp(54), 1f))
        tmap.setOnClickListener { chooseBuiltIn(0) }
        naver.setOnClickListener { chooseBuiltIn(2) }
        kakao.setOnClickListener { chooseBuiltIn(1) }
        add.setOnClickListener { chooseCustomNavigation() }
        page.addView(navCard)

        val shutdownCard = card()
        autoShutdown = CheckBox(this).apply {
            text = "충전 종료 시 휴대폰 자동종료"
            textSize = 19f
            setTextColor(Color.BLACK)
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
            isChecked = prefs.getBoolean(KEY_SHUTDOWN, false)
        }
        shutdownCard.addView(autoShutdown)
        shutdownCard.addView(description("충전 케이블을 분리하면 내비게이션을 종료하고\n시스템 전원 메뉴를 호출합니다."))
        page.addView(shutdownCard)

        val save = Button(this).apply {
            text = "설정 저장"
            textSize = 19f
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = rounded(Color.BLACK, 100)
            setOnClickListener { saveSettings() }
        }
        val saveParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(62))
        saveParams.topMargin = dp(20)
        page.addView(save, saveParams)

        status = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        }
        page.addView(status)
        page.addView(TextView(this).apply {
            text = "부팅 후 자동 실행 · 광고 없음"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(2))
        })

        scroll.addView(page)
        setContentView(scroll)
        refreshNavigationSelection()
    }

    private fun card(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(Color.rgb(250, 250, 250), 24, Color.rgb(222, 222, 222), dp(1))
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(8)
        params.bottomMargin = dp(4)
        return box.also { it.layoutParams = params }
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 21f
        setTextColor(Color.BLACK)
        setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(12), 0, dp(10))
    }

    private fun description(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(Color.GRAY)
        setPadding(dp(2), dp(6), 0, dp(2))
        setLineSpacing(dp(4).toFloat(), 1f)
    }

    private fun navRadio(text: String) = RadioButton(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(Color.BLACK)
        buttonTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
        setPadding(0, 0, dp(4), 0)
        isSingleLine = false
    }

    private fun chooseBuiltIn(index: Int) {
        val app = builtInApps[index]
        selectedPackage = app.packageName
        selectedLabel = app.label
        refreshNavigationSelection()
    }

    private fun chooseCustomNavigation() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (apps.isEmpty()) {
            Toast.makeText(this, "선택할 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
            refreshNavigationSelection()
            return
        }
        val labels = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("내비게이션 앱 선택")
            .setItems(labels) { _, which ->
                selectedPackage = apps[which].activityInfo.packageName
                selectedLabel = labels[which]
                refreshNavigationSelection()
            }
            .setNegativeButton("취소") { _, _ -> refreshNavigationSelection() }
            .show()
    }

    private fun refreshNavigationSelection() {
        val index = when (selectedPackage) {
            builtInApps[0].packageName -> 0
            builtInApps[2].packageName -> 1
            builtInApps[1].packageName -> 2
            else -> 3
        }
        navButtons.forEachIndexed { i, button -> button.isChecked = i == index }
    }

    private fun saveSettings() {
        prefs.edit()
            .putString(KEY_PACKAGE, selectedPackage)
            .putString(KEY_LABEL, selectedLabel)
            .putBoolean(KEY_AUTO, autoLaunch.isChecked)
            .putBoolean(KEY_SHUTDOWN, autoShutdown.isChecked)
            .putBoolean(KEY_RESTORE_LAST_LOCATION, true)
            .apply()
        status.text = "설정을 저장했습니다."
    }

    private fun showAppInfo() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.2.6"
        } catch (_: Exception) { "0.2.6" }
        AlertDialog.Builder(this)
            .setTitle("앱 정보")
            .setMessage("Easy Navi Starter\n버전 $version\nAndroid\n\nRyoo jaesang & 최피디")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(strokeWidth, stroke)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "easy_navi_settings"
        const val KEY_PACKAGE = "navigation_package"
        const val KEY_LABEL = "navigation_label"
        const val KEY_AUTO = "auto_launch"
        const val KEY_SHUTDOWN = "auto_shutdown_on_unplug"
        const val KEY_RESTORE_LAST_LOCATION = "restore_last_location"
        const val KEY_BOOT = "restore_on_boot"
    }
}
