# 이지 네비 스타터 (Easy Navi Starter)

## 프로젝트 목적
차량 USB 전원 연결을 감지해 사용자가 선택한 Android 내비게이션 앱의 실행을 보조하는 앱입니다.

## 개발 환경
- 저장소: `ryoobach/Easy_Navi_Starter`
- 기본 브랜치: `main`
- 플랫폼: Android
- 언어: Kotlin
- 빌드: Gradle / GitHub Actions

## 0.2.3 반영 사항
- 앱 버전: 0.2.3 (versionCode 5)
- 내비게이션 선택: 티맵, 카카오맵, 네이버 지도 라디오버튼 3개를 기본 화면에 표시
- 선택한 내비게이션 설정 저장 및 다음 실행 때 복원
- 기기 시작 시 내비게이션 자동 실행은 기본 동작으로 유지
- 앱 식별자(`applicationId`) 유지: `com.ryoobach.easynavistarter`
- APK 파일명: `Easy-Navi-Starter-v0.2.3-release.apk`

## APK 서명 및 업데이트 설치
Android는 기존 앱과 새 APK의 서명 인증서가 일치해야 앱 위에 업데이트 설치를 허용합니다. 앱 식별자도 계속 `com.ryoobach.easynavistarter`로 유지하고, 매 버전 `versionCode`를 올려야 합니다.

GitHub 저장소 Settings → Secrets and variables → Actions에 다음 저장소 비밀값을 등록해야 합니다.
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

이 프로젝트는 위 Release 키를 계속 사용하도록 구성되어 있습니다. 키를 새로 만들거나 교체하면 기존 설치본에 업데이트가 되지 않을 수 있으므로 기존 키와 비밀번호를 안전하게 보관해야 합니다. 빌드 작업은 매번 같은 키를 불러오고, APK의 서명 인증서 SHA-256 지문이 키 저장소의 지문과 일치하는지 확인합니다.

기존 0.2.0은 임시 Debug 서명으로 빌드됐고 해당 키가 보존되지 않았습니다. 따라서 0.2.0 설치본은 Release 서명 버전으로 바로 덮어쓰기 업데이트할 수 없으며, 최초 1회 기존 앱을 제거한 뒤 설치해야 할 수 있습니다. 이 과정에서 앱 설정 데이터가 삭제될 수 있습니다. 0.2.1 또는 0.2.2가 현재 저장소의 동일한 Release 키로 서명되어 설치된 경우에는 새 버전도 동일 키를 사용하는 한 앱을 제거하지 않고 업데이트할 수 있습니다.

앱 제거가 설정 화면에서 되지 않으면 Android 설정 → 애플리케이션 → 이지 네비 스타터 → 삭제 경로를 이용해야 합니다. 일반 Android 앱은 다른 앱을 강제로 삭제할 수 없습니다.

## 빌드 검증
GitHub Actions는 Gradle 설정에서 앱 버전과 식별자를 읽고 다음 항목을 확인한 뒤 APK를 업로드합니다.
1. 앱 식별자가 `com.ryoobach.easynavistarter`인지 확인
2. APK 내부의 `applicationId`, `versionName`, `versionCode`가 Gradle 설정과 일치하는지 확인
3. APK 서명 인증서 SHA-256 지문이 보관된 Release 키의 지문과 일치하는지 확인
4. 버전이 포함된 파일명으로 APK 아티팩트 생성

## 확정 요구사항 및 기술 제약
1. 내비게이션 선택은 티맵, 카카오맵, 네이버 지도 라디오버튼 3개를 기본 화면에 표시합니다.
2. 저장한 내비게이션 선택은 재실행 후에도 유지합니다.
3. 기기 시작 시 저장된 내비게이션을 자동 실행하는 것을 기본 동작으로 합니다.
4. 일반 Android 앱은 다른 앱을 강제로 종료하거나 휴대폰 전원을 완전히 끌 수 없습니다.
5. USB 해제만으로 차량 시동 OFF라고 단정할 수 없습니다. 차량 충전기 구분 및 지연 처리, 화면 제어는 실기기 검증이 필요합니다.
6. Android 백그라운드 실행 제한으로 자동 실행은 기기·OS별 검증이 필요합니다.

## 버전 관리
- 0.1.0 (versionCode 1): 초기 테스트 앱
- 0.2.0 (versionCode 2): 설정 화면 정리 및 앱 아이콘 적용
- 0.2.1 (versionCode 3): Release 서명 및 버전별 APK 파일명 적용
- 0.2.2 (versionCode 4): 내비게이션 라디오버튼 화면 수정
- 0.2.3 (versionCode 5): 지속 서명 키 및 APK 내부 정보 검증 강화

버전 변경 시 앱 화면의 버전, `versionName`, `versionCode`, 앱 정보, README와 APK 파일명을 함께 갱신합니다.
