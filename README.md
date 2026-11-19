# 이지 네비 스타터 (Easy Navi Starter)

## 프로젝트 목적
차량 USB 전원 연결을 감지해 사용자가 선택한 Android 내비게이션 앱의 실행을 보조하는 앱입니다.

## 개발 환경
- 저장소: `ryoobach/Easy_Navi_Starter`
- 기본 브랜치: `main`
- 플랫폼: Android
- 언어: Kotlin
- 빌드: Gradle / GitHub Actions
- 기준 테스트 기기: Samsung Galaxy Tab A11

## 0.2.2 반영 사항
- 앱 버전: 0.2.2 (versionCode 4)
- 내비게이션 선택을 드롭다운에서 라디오버튼 3개로 변경
- 기본 화면에 티맵, 카카오맵, 네이버 지도 항목을 항상 표시
- 선택한 내비게이션 설정을 저장하고 다음 실행 때 복원
- 기기 시작 시 내비게이션 자동 실행은 기본 동작으로 유지하며 별도 체크 항목 제거
- 앱 식별자(`applicationId`) 유지: `com.ryoobach.easynavistarter`
- APK 파일명: `Easy-Navi-Starter-v0.2.2-release.apk`

## 업데이트 설치 시 주의사항
Android는 기존 앱과 새 APK의 서명이 다르면 앱 위에 업데이트 설치를 허용하지 않습니다. 모든 후속 버전은 동일한 Release 서명 키를 사용해야 합니다.

GitHub 저장소 Settings → Secrets and variables → Actions에 다음 저장소 비밀값을 등록해야 합니다.
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

**기존 0.2.0은 임시 Debug 서명으로 빌드됐고 그 서명 키는 보존되지 않았습니다.** 따라서 0.2.0에서 0.2.1/0.2.2 Release 서명판으로는 바로 덮어쓰기 업데이트할 수 없습니다. 최초 1회 기존 앱을 제거한 뒤 설치해야 하며, 이 과정에서 앱 설정 데이터가 삭제될 수 있습니다. 이후 동일한 Release 키를 유지하는 버전끼리는 앱을 삭제하지 않고 업데이트할 수 있습니다.

앱 제거가 설정 화면에서 되지 않으면 Android 설정 → 애플리케이션 → 이지 네비 스타터 → 삭제 경로를 이용해야 합니다. 기기 관리자 등 특별 권한을 앱이 요청하지 않는 한 앱 자체에서 다른 앱을 강제로 삭제할 수는 없습니다.

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
- 0.2.1 (versionCode 3): 지속형 Release 서명 및 버전별 APK 파일명 적용
- 0.2.2 (versionCode 4): 내비게이션 라디오버튼 화면 수정

버전 변경 시 앱 화면의 버전, `versionName`, `versionCode`, 앱 정보, README와 APK 파일명을 함께 갱신합니다.
