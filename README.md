# 이지 네비 스타터 (Easy Navi Starter)

## 프로젝트 목적
차량 USB 전원 연결을 감지해 사용자가 지정한 Android 내비게이션 앱의 실행을 보조하는 앱입니다.

## 개발 환경
- 저장소: `ryoobach/Easy_Navi_Starter`
- 기본 브랜치: `main`
- 플랫폼: Android
- 언어: Kotlin
- 빌드: Gradle / GitHub Actions
- 기준 테스트 기기: Samsung Galaxy Tab A11

## 0.2.1 반영 사항
- 앱 버전: 0.2.1 (versionCode 3)
- 앱 식별자(`applicationId`)를 `com.ryoobach.easynavistarter`로 유지
- GitHub Actions의 매 빌드 임시 Debug 서명 방식 제거
- 지속적으로 같은 비공개 Release 서명 키를 사용하는 APK 빌드로 변경
- 다운로드 파일명에 버전과 빌드 종류를 포함: `Easy-Navi-Starter-v0.2.1-release.apk`
- 앱 화면과 앱 정보의 버전 표시 동기화

## 업데이트 설치를 위한 서명 키 설정
Android는 기존 앱과 새 APK의 서명이 다르면 앱 위에 업데이트 설치를 허용하지 않습니다. Release 서명 키는 최초 한 번 생성한 뒤 안전하게 보관하고, 이후 모든 버전에서 반드시 같은 키를 사용해야 합니다.

GitHub 저장소의 Settings → Secrets and variables → Actions에 다음 저장소 비밀값을 등록합니다.
- `ANDROID_KEYSTORE_BASE64`: Release keystore 파일의 Base64 문자열
- `ANDROID_KEYSTORE_PASSWORD`: keystore 비밀번호
- `ANDROID_KEY_ALIAS`: 키 별칭
- `ANDROID_KEY_PASSWORD`: 키 비밀번호

키 파일은 저장소에 올리지 말고 안전한 별도 장소에 백업합니다. 키를 잃어버리면 해당 키로 설치한 앱의 업데이트를 만들 수 없습니다.

## 중요: 현재 설치된 0.2.0 앱에서 0.2.1로 넘어갈 때
기존 0.2.0은 GitHub Actions의 임시 Debug 서명 키로 만들어졌습니다. 그 키는 빌드 환경에 영구 보관되지 않았으므로, 새로 만든 Release 키와 서명이 다릅니다. 따라서 **현재 설치본에서 새 Release 키로 만든 첫 APK로는 덮어쓰기 업데이트가 불가능하며, 최초 1회 기존 앱을 제거한 뒤 새 APK를 설치해야 합니다.** 이 과정에서 앱 내부 설정 데이터가 삭제될 수 있으므로, 필요한 설정을 먼저 기록해 두십시오.

0.2.1 Release 설치 이후에는 같은 Release 키로 서명한 후속 버전을 기존 앱 삭제 없이 업데이트할 수 있습니다. 서명 키를 변경하거나 잃어버리지 않습니다.

## 확정 요구사항 및 기술 제약
1. 차량 USB 전원 연결을 시동 감지 신호로 우선 활용합니다.
2. 설정한 내비게이션 앱을 실행할 수 있도록 합니다.
3. 앱 설정은 재부팅 후에도 유지합니다.
4. 마지막 위치 복원은 기본 적용합니다.
5. 일반 Android 앱은 다른 앱을 강제로 종료하거나 휴대폰 전원을 완전히 끌 수 없습니다.
6. USB 해제만으로 차량 시동 OFF라고 단정할 수 없습니다. 차량 충전기 구분 및 지연 처리, 화면 제어는 실기기 검증과 별도 권한 검토가 필요합니다.
7. Android 백그라운드 실행 제한으로 자동 실행은 기기·OS별 검증이 필요합니다.

## 버전 관리
- 0.1.0 (versionCode 1): 초기 테스트 앱
- 0.2.0 (versionCode 2): 설정 화면 정리 및 앱 아이콘 적용
- 0.2.1 (versionCode 3): 지속형 Release 서명 및 버전별 APK 파일명 적용

버전 변경 시 앱 화면의 버전, `versionName`, `versionCode`, 앱 정보, README와 APK 파일명을 함께 갱신합니다.
