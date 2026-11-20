package com.ryoobach.easynavistarter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.view.Gravity
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
    private lateinit var overlayPermissionButton: Button
    private lateinit var status: TextView
    private lateinit var bootDiagnostics: TextView
    private lateinit var navListContainer: LinearLayout
    private var selectedPackage = ""
    private var selectedLabel = ""
    private val addedNavigationApps = mutableListOf<NavApp>()

    private data class NavApp(val label: String, val packageName: String)

    // 국내외 주요 지도·내비게이션 앱 후보. 실제 설치 및 실행 가능한 앱만 표시합니다.
    private val navigationCandidates = listOf(
        "com.skt.tmap.ku",                 // 티맵
        "com.skt.tmap",                    // 구형 티맵
        "com.locnall.KimGiSa",             // 카카오내비
        "net.daum.android.map",            // 카카오맵
        "com.nhn.android.nmap",            // 네이버지도
        "kr.mappers.AtlanSmart",           // 아틀란
        "com.thinkware.inaviair",          // 아이나비 에어
        "com.google.android.apps.maps",    // Google 지도
        "com.waze",                        // Waze
        "net.osmand",                      // OsmAnd
        "com.sygic.aura",                  // Sygic GPS Navigation
        "com.here.app.maps"                // HERE WeGo
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        DiagnosticLogger.log(this, "이지 네비 스타터 앱 시작")
        val storedPackage = prefs.getString(KEY_PACKAGE, "") ?: ""
        // 구버전에서 저장한 티맵 패키지명을 현재 패키지명으로 이전합니다.
        selectedPackage = if (
            storedPackage == "com.skt.tmap" &&
            packageManager.getLaunchIntentForPackage("com.skt.tmap.ku") != null
        ) "com.skt.tmap.ku" else storedPackage
        selectedLabel = prefs.getString(KEY_LABEL, "") ?: ""
        loadAddedNavigationApps()
        // 저장된 실행 앱이 더 이상 설치되어 있거나 추가 목록에 없으면 선택을 비웁니다.
        if (addedNavigationApps.none { it.packageName == selectedPackage }) {
            selectedPackage = ""
            selectedLabel = ""
        } else if (selectedPackage != storedPackage) {
            prefs.edit().putString(KEY_PACKAGE, selectedPackage).putString(KEY_LABEL, selectedLabel).apply()
        }

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
            setImageResource(com.ryoobach.easynavistarter.R.drawable.easy_navi_launcher)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(60), dp(60)))
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
        bootCard.addView(description("스마트 기기를 켜면 부팅이 되면서 아래의\n내비가 자동 실행됩니다.\n화면 자동 전환을 위해 아래 권한을 허용해 주세요."))
        overlayPermissionButton = Button(this).apply {
            text = if (Settings.canDrawOverlays(this@MainActivity)) {
                "부팅 화면 표시 권한 허용됨"
            } else {
                "부팅 화면 표시 권한 설정"
            }
            isAllCaps = false
            setOnClickListener { openOverlayPermissionSettings() }
        }
        bootCard.addView(overlayPermissionButton)
        page.addView(bootCard)

        page.addView(sectionTitle("시스템 진단"))
        val diagnosticCard = card()
        bootDiagnostics = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        diagnosticCard.addView(bootDiagnostics)
        diagnosticCard.addView(Button(this).apply {
            text = "진단 내용 TXT 저장"
            isAllCaps = false
            setOnClickListener { exportSystemDiagnostics() }
        })
        page.addView(diagnosticCard)
        refreshBootDiagnostics()

        page.addView(sectionTitle("내비게이션 선택"))
        val navCard = card()
        navListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        navCard.addView(navListContainer)
        refreshNavigationList()
        page.addView(navCard)

        val shutdownNotice = TextView(this).apply {
            text = "충전 케이블 분리 시 내비게이션 종료 및 전원 메뉴 표시 기능은 보안 차단 문제를 해결하기 위해 이번 버전에서 제외했습니다."
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(dp(4), dp(10), dp(4), dp(4))
        }
        page.addView(shutdownNotice)

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
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        DiagnosticLogger.log(this, "앱 화면 복귀 또는 표시(onResume)")
        if (::bootDiagnostics.isInitialized) refreshBootDiagnostics()
        if (::overlayPermissionButton.isInitialized) {
            overlayPermissionButton.text = if (Settings.canDrawOverlays(this)) {
                "부팅 화면 표시 권한 허용됨"
            } else {
                "부팅 화면 표시 권한 설정"
            }
        }
    }

    override fun onPause() {
        DiagnosticLogger.log(this, "앱 화면 일시중지(onPause)")
        super.onPause()
    }

    override fun onStop() {
        DiagnosticLogger.log(this, "앱 화면 가려짐(onStop)")
        super.onStop()
    }

    private fun exportSystemDiagnostics() {
        try {
            val fileName = "EasyNavi_Diagnostic_" +
                java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.KOREA).format(java.util.Date()) + ".txt"
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EasyNaviStarter")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("다운로드 폴더 파일을 만들지 못했습니다.")
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(DiagnosticLogger.readAll(this))
                } ?: throw IllegalStateException("파일을 기록하지 못했습니다.")
                Toast.makeText(this, "다운로드/EasyNaviStarter에 저장했습니다.\n$fileName", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                startActivityForResult(intent, 2402)
            }
            DiagnosticLogger.log(this, "시스템 진단 TXT 내보내기 요청", fileName)
        } catch (e: Exception) {
            Toast.makeText(this, "진단 파일 저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
            DiagnosticLogger.log(this, "시스템 진단 TXT 저장 오류", e.message ?: e.javaClass.simpleName)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2402 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(DiagnosticLogger.readAll(this))
                }
                Toast.makeText(this, "진단 파일을 저장했습니다.", Toast.LENGTH_LONG).show()
                DiagnosticLogger.log(this, "시스템 진단 TXT 저장 완료", uri.toString())
            } catch (e: Exception) {
                Toast.makeText(this, "진단 파일 저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshBootDiagnostics() {
        val bootTime = prefs.getLong(KEY_DIAG_BOOT_TIME, 0L)
        val attemptTime = prefs.getLong(KEY_DIAG_ATTEMPT_TIME, 0L)
        val resultTime = prefs.getLong(KEY_DIAG_RESULT_TIME, 0L)
        val bootPackage = prefs.getString(KEY_DIAG_BOOT_PACKAGE, "") ?: ""
        val result = prefs.getString(KEY_DIAG_RESULT, "아직 부팅 진단 기록이 없습니다.") ?: ""
        fun displayTime(value: Long): String =
            if (value <= 0L) "기록 없음"
            else java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.KOREA)
                .format(java.util.Date(value))
        bootDiagnostics.text = "마지막 부팅 이벤트: ${displayTime(bootTime)}\n" +
            "부팅 당시 저장된 앱: ${if (bootPackage.isBlank()) "없음" else bootPackage}\n" +
            "실행 요청 시각: ${displayTime(attemptTime)}\n" +
            "결과 기록 시각: ${displayTime(resultTime)}\n" +
            "진단 결과: $result\n\n" +
            "※ '실행 요청 전송됨'은 Android에 실행을 요청했다는 뜻이며, 실제 화면 표시를 보장하지 않습니다."
    }

    private fun openOverlayPermissionSettings() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "화면 자동 표시 권한이 이미 허용되어 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("화면 자동 표시 권한")
            .setMessage("기기를 재시작했을 때 티맵 화면을 앞으로 표시하려면 안드로이드의 특별 접근 권한이 필요합니다. 설정 화면에서 이지 네비 스타터의 '다른 앱 위에 표시'를 허용해 주세요.")
            .setPositiveButton("권한 설정") { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1201)
        }
    }

    private fun loadAddedNavigationApps() {
        val saved = prefs.getStringSet(KEY_ADDED_NAV_APPS, emptySet())?.toSet() ?: emptySet()
        val installedApps = findInstalledNavigationApps()
        addedNavigationApps.clear()
        saved.forEach { packageName ->
            // 구버전 티맵 패키지 저장값도 현재 설치된 티맵으로 복구합니다.
            val app = installedApps.firstOrNull { it.packageName == packageName }
                ?: if (packageName == "com.skt.tmap") {
                    installedApps.firstOrNull { it.packageName == "com.skt.tmap.ku" }
                } else null
            if (app != null && addedNavigationApps.none { it.packageName == app.packageName }) {
                addedNavigationApps.add(app)
            }
        }
        prefs.edit().putStringSet(KEY_ADDED_NAV_APPS, addedNavigationApps.map { it.packageName }.toSet()).apply()
    }

    // 사전에 등록한 패키지 후보와 기기의 설치 앱을 대조합니다.
    // 패키지 가시성은 AndroidManifest의 MAIN/LAUNCHER queries로 확보합니다.
    private fun findInstalledNavigationApps(): List<NavApp> {
        return navigationCandidates.mapNotNull { candidatePackage ->
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= 33) {
                    packageManager.getApplicationInfo(
                        candidatePackage,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(candidatePackage, 0)
                }
                if (candidatePackage == packageName ||
                    packageManager.getLaunchIntentForPackage(candidatePackage) == null
                ) return@mapNotNull null

                val label = packageManager.getApplicationLabel(appInfo).toString()
                NavApp(label, candidatePackage)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: Exception) {
                null
            }
        }.distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun showAddNavigationDialog() {
        val installedApps = findInstalledNavigationApps()
        if (installedApps.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("내비게이션 앱 없음")
                .setMessage("설치된 내비게이션 앱이 없습니다.\n내비게이션 앱을 설치한 후 다시 시도해 주세요.")
                .setPositiveButton("확인", null)
                .show()
            return
        }

        val labels = installedApps.map { it.label }.toTypedArray()
        val selected = BooleanArray(installedApps.size) { index ->
            addedNavigationApps.any { it.packageName == installedApps[index].packageName }
        }
        AlertDialog.Builder(this)
            .setTitle("설치된 내비게이션 앱")
            .setMultiChoiceItems(labels, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            .setPositiveButton("추가") { _, _ ->
                addedNavigationApps.clear()
                installedApps.forEachIndexed { index, app ->
                    if (selected[index]) addedNavigationApps.add(app)
                }
                prefs.edit().putStringSet(KEY_ADDED_NAV_APPS, addedNavigationApps.map { it.packageName }.toSet()).apply()
                if (addedNavigationApps.none { it.packageName == selectedPackage }) {
                    selectedPackage = ""
                    selectedLabel = ""
                }
                refreshNavigationList()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun refreshNavigationList() {
        navListContainer.removeAllViews()
        val allApps = addedNavigationApps.toList()
        allApps.forEach { app ->
            val radio = RadioButton(this).apply {
                text = app.label
                textSize = 18f
                setTextColor(Color.BLACK)
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
                isChecked = selectedPackage == app.packageName
                setPadding(0, 0, dp(4), 0)
                setOnClickListener {
                    selectedPackage = app.packageName
                    selectedLabel = app.label
                    refreshNavigationList()
                }
            }
            navListContainer.addView(radio, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
            ))
        }
        val addButton = Button(this).apply {
            text = "네비 추가"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.BLACK)
            background = rounded(Color.rgb(238, 238, 238), 12)
            setOnClickListener { showAddNavigationDialog() }
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
        params.topMargin = dp(6)
        navListContainer.addView(addButton, params)
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

    private fun saveSettings() {
        if (selectedPackage.isBlank() || addedNavigationApps.none { it.packageName == selectedPackage }) {
            Toast.makeText(this, "네비 추가에서 실행할 내비게이션을 등록하고 선택해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        val saved = prefs.edit()
            .putString(KEY_PACKAGE, selectedPackage)
            .putString(KEY_LABEL, selectedLabel)
            .putStringSet(KEY_ADDED_NAV_APPS, addedNavigationApps.map { it.packageName }.toSet())
            .putBoolean(KEY_AUTO, autoLaunch.isChecked)
                        .putBoolean(KEY_RESTORE_LAST_LOCATION, true)
            .commit()
        if (!saved) {
            status.text = "설정을 저장하지 못했습니다."
            return
        }
        status.text = "설정을 저장했습니다."
        DiagnosticLogger.log(this, "설정 저장", "내비게이션=$selectedLabel ($selectedPackage); 부팅 자동 실행=${autoLaunch.isChecked}")
        val launchIntent = getNavigationLaunchIntent(selectedPackage)
        if (launchIntent == null) {
            Toast.makeText(this, "선택한 내비게이션 앱을 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            DiagnosticLogger.log(this, "사용자 설정 저장 후 내비게이션 실행 요청", selectedPackage)
            startActivity(launchIntent)
        } catch (e: Exception) {
            DiagnosticLogger.log(this, "내비게이션 직접 실행 오류", e.message ?: e.javaClass.simpleName)
            Toast.makeText(this, "내비게이션 앱을 불러오지 못했습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getNavigationLaunchIntent(packageName: String): Intent? {
        // 현재 티맵과 구형 티맵 패키지명을 모두 지원합니다.
        val candidates = if (packageName == "com.skt.tmap.ku" || packageName == "com.skt.tmap") {
            listOf(packageName, "com.skt.tmap.ku", "com.skt.tmap").distinct()
        } else {
            listOf(packageName)
        }
        return candidates.firstNotNullOfOrNull { candidate ->
            packageManager.getLaunchIntentForPackage(candidate)
        }?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun showAppInfo() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.4.2"
        } catch (_: Exception) { "0.4.2" }
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        // 첫 번째 줄: 앱 아이콘 + 앱 이름, 그 아래 작은 버전 표기
        val appInfoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val appLogo = ImageView(this).apply {
            setImageResource(R.drawable.easy_navi_launcher)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Easy Navi Starter 앱 아이콘"
        }
        appInfoRow.addView(appLogo, LinearLayout.LayoutParams(dp(56), dp(56)))
        val appText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        appText.addView(TextView(this).apply {
            text = "Easy Navi Starter"
            textSize = 19f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        })
        appText.addView(TextView(this).apply {
            text = "버전 $version"
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, dp(4), 0, 0)
        })
        appText.addView(TextView(this).apply {
            text = "개발일 2026.11.20"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(2), 0, 0)
        })
        appInfoRow.addView(appText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        infoLayout.addView(appInfoRow)

        val separator = TextView(this).apply {
            text = ""
            setBackgroundColor(Color.rgb(225, 225, 225))
        }
        val separatorParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        separatorParams.topMargin = dp(16)
        separatorParams.bottomMargin = dp(16)
        infoLayout.addView(separator, separatorParams)

        // 두 번째 줄: JS 협업 로고 + 협업자 이름
        val collaborationRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val collaborationLogo = ImageView(this).apply {
            setImageResource(R.drawable.js_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "JS 협업 로고"
        }
        collaborationRow.addView(collaborationLogo, LinearLayout.LayoutParams(dp(40), dp(40)))
        collaborationRow.addView(TextView(this).apply {
            text = "Ryoo jaesang & 최피디"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(dp(14), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        infoLayout.addView(collaborationRow)
        AlertDialog.Builder(this)
            .setTitle("앱 정보")
            .setView(infoLayout)
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
        const val KEY_ADDED_NAV_APPS = "added_navigation_apps"
        const val KEY_AUTO = "auto_launch"
        const val KEY_SHUTDOWN = "auto_shutdown_on_unplug"
        const val KEY_RESTORE_LAST_LOCATION = "restore_last_location"
        const val KEY_BOOT = "restore_on_boot"
        const val KEY_DIAG_BOOT_TIME = "diag_boot_time"
        const val KEY_DIAG_ATTEMPT_TIME = "diag_attempt_time"
        const val KEY_DIAG_RESULT_TIME = "diag_result_time"
        const val KEY_DIAG_BOOT_PACKAGE = "diag_boot_package"
        const val KEY_DIAG_RESULT = "diag_result"
    }
}
