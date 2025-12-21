# 📰 마이 뉴스 (MyNews)

> **내가 원하는 뉴스만 쏙!**
> 실시간 뉴스를 카테고리별로 제공하고, NFC로 친구와 뉴스를 공유하는 안드로이드 앱

<div align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=material-design&logoColor=white" />
</div>

---

## 📋 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술적 도전과 해결](#-기술적-도전과-해결)
- [기술 스택](#-기술-스택)
- [아키텍처](#️-아키텍처)
- [시작하기](#-시작하기)
- [팀원](#-팀원)

---

## 🎯 프로젝트 소개

**마이 뉴스**는 사용자가 원하는 카테고리의 실시간 뉴스를 빠르게 확인하고, NFC 기술을 활용하여 친구들과 기사를 공유할 수 있는 모바일 뉴스 애플리케이션입니다.

### ✨ 핵심 가치

- **빠른 뉴스 접근**: 캐싱 전략을 통한 즉각적인 뉴스 로딩
- **개인화된 경험**: 북마크와 하이라이트로 나만의 뉴스 큐레이션
- **소셜 기능**: NFC를 통한 오프라인 뉴스 공유
- **여론 확인**: 실시간 좋아요/싫어요를 통한 대중의 반응 파악

---

## 🚀 주요 기능

### 1️⃣ 카테고리별 뉴스 조회
- **추천 뉴스**: 7초마다 자동 업데이트되는 메인 추천 뉴스
- **카테고리별 최신 뉴스**: 정치, 경제, 사회, 기술 등 다양한 섹션
- **캐싱 전략**: 로컬 캐시 우선 표시 후 비동기 API 호출로 데이터 병합

### 2️⃣ NFC 뉴스 공유
- **기기 간 직접 공유**: 스마트폰을 가까이 대면 뉴스 기사 즉시 전송
- **Foreground Dispatch System**: 앱이 실행 중일 때만 작동하여 안정성 보장
- **딥링크 지원**: `nfcnews://article/{articleId}` 스킴으로 직접 기사 이동

### 3️⃣ 뉴스 반응 (좋아요/싫어요)
- **낙관적 UI 업데이트**: 즉각적인 UI 반영 후 백그라운드 동기화
- **연타 방지**: 0.5초 간격 제한으로 서버 부하 및 데이터 정합성 보장
- **3가지 상태 관리**: Like / Unlike / None

### 4️⃣ 북마크 & 하이라이트
- **북마크**: 관심 있는 기사를 저장하여 언제든 다시 보기
- **하이라이트**: 기사 내 중요한 부분을 형광펜처럼 표시
- **로컬 저장**: Room Database로 빠른 조회 및 오프라인 지원

### 5️⃣ 사용자 프로필
- **간편한 회원가입**: 이메일 기반 회원가입
- **내 활동 관리**: 북마크 목록, 하이라이트 기사 모아보기

---

## 💡 기술적 도전과 해결

### 🔥 도전 1: API 호출 병목 현상

**문제**
매번 뉴스를 API로 호출하면 사용자가 앱 이용 시 병목이 발생

**해결**
```kotlin
// 1. 캐시 데이터를 먼저 보여줌
val cachedNews = articleCache.getArticles(section)
_uiState.value = _uiState.value.copy(articles = cachedNews)

// 2. 비동기로 API 호출하여 최신 데이터 병합
viewModelScope.launch {
    val freshNews = repository.getArticles(section)
    articleCache.updateArticles(section, freshNews)
    _uiState.value = _uiState.value.copy(articles = freshNews)
}
```

### 🔥 도전 2: 추천 뉴스의 실시간성

**문제**
최상단 추천 뉴스도 매번 API 호출하면 성능 저하

**해결**
- 추천 뉴스도 캐싱 적용
- 백그라운드에서 7초마다 자동 업데이트
- `LaunchedEffect`와 `delay`를 활용한 주기적 갱신

### 🔥 도전 3: NFC Intent 충돌

**문제**
안드로이드의 수많은 기본 Intent로 인해 우리 앱의 NFC 로직이 실행되지 않음

**해결**
- **Foreground Dispatch System** 적용
- 앱이 Foreground에 있을 때만 NFC 공유 가능하게 제한
- `enableForegroundDispatch()` 및 `disableForegroundDispatch()` 생명주기 관리

```kotlin
override fun onResume() {
    super.onResume()
    nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
}

override fun onPause() {
    super.onPause()
    nfcAdapter?.disableForegroundDispatch(this)
}
```

### 🔥 도전 4: 좋아요/싫어요 연타 문제

**문제**
사용자의 반복 클릭이 그대로 서버에 반영되어 서버 부하 및 데이터 정합성 문제 발생

**해결**
- 클릭 간격 0.5초 제한
- Like / Unlike / None 3가지 상태로 관리
- 낙관적 UI 업데이트로 즉각적인 피드백 제공

```kotlin
private var lastClickTime = 0L

fun onReactionClick(type: ReactionType) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastClickTime < 500) return // 0.5초 제한
    lastClickTime = currentTime

    // 낙관적 업데이트
    updateLocalReaction(type)

    // 백그라운드 동기화
    viewModelScope.launch {
        repository.updateReaction(articleId, type)
    }
}
```

---

## 🛠 기술 스택

### 개발 언어 & UI
- **Kotlin**: 안드로이드 공식 언어
- **Jetpack Compose**: 선언형 UI 프레임워크
- **Material Design 3**: 최신 구글 디자인 시스템

### 아키텍처
- **Clean Architecture**: UI, Domain, Data 레이어 분리
- **MVVM Pattern**: ViewModel을 통한 상태 관리
- **Multi-Module**: 기능별 모듈화
- **Unidirectional Data Flow**: StateFlow를 통한 단방향 데이터 흐름

### 네트워크 & 데이터
| 분류 | 기술 스택 | 설명 |
|------|-----------|------|
| **HTTP Client** | Retrofit2 | REST API 통신 |
| **JSON Parser** | Moshi | JSON ↔ Kotlin Object 변환 |
| **HTTP Logger** | OkHttp + Interceptor | 네트워크 로깅 및 JWT 자동 주입 |
| **Async** | Kotlin Coroutines | 비동기 처리 |
| **State** | StateFlow | 반응형 상태 관리 |

### 로컬 저장소
- **Room Database**: 하이라이트 데이터 저장 (RDB 유사)
- **DataStore**: JWT Token, 좋아요/싫어요/북마크 ID 저장 (Key-Value)
- **StateFlow Cache**: 메모리 캐싱

### 의존성 주입
- **Hilt**: Android 전용 DI 라이브러리
  - `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`
  - Dagger 기반의 컴파일 타임 DI

### 기타
- **NFC (HCE)**: Host Card Emulation을 통한 뉴스 공유
- **Coil**: 이미지 로딩 라이브러리
- **Navigation Compose**: 화면 전환 관리

---

## 🏗️ 아키텍처

### 시스템 아키텍처

```
┌─────────────┐
│   Android   │
│ Application │
└──────┬──────┘
       │ API
       ▼
┌─────────────────────────────────┐
│  Google Cloud Platform (GCP)    │
│  ┌──────────┐   ┌────────────┐  │
│  │Cloud Run │───│ Cloud SQL  │  │
│  │(WAS/API) │   │  (MySQL)   │  │
│  └──────────┘   └────────────┘  │
│  ┌──────────┐   ┌────────────┐  │
│  │  Secret  │   │Cloud       │  │
│  │ Manager  │   │Logging     │  │
│  └──────────┘   └────────────┘  │
└─────────────────────────────────┘
```

### Android App 아키텍처 (MVVM + Clean Architecture)

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │   View   │←─│  ViewModel   │←─│   UseCase        │   │
│  │(Compose) │  │ (StateFlow)  │  │ (Business Logic) │   │
│  └──────────┘  └──────────────┘  └──────────────────┘   │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                      Domain Layer                        │
│  ┌──────────────────┐  ┌──────────────────────────┐     │
│  │   Repository     │  │     Domain Model         │     │
│  │   Interface      │  │  (Business Entities)     │     │
│  └──────────────────┘  └──────────────────────────┘     │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                       Data Layer                         │
│  ┌──────────────────┐  ┌──────────────────────────┐     │
│  │   Repository     │  │    Data Source           │     │
│  │ Implementation   │  │ (Remote/Local/Cache)     │     │
│  └────────┬─────────┘  └──────────────────────────┘     │
│           │                                              │
│  ┌────────▼──────┐  ┌──────────┐  ┌──────────────┐     │
│  │  ApiService   │  │   Room   │  │   DataStore  │     │
│  │  (Retrofit)   │  │   (DB)   │  │ (Preferences)│     │
│  └───────────────┘  └──────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### 데이터 흐름 (낙관적 업데이트 적용)

```
사용자 액션
    │
    ▼
┌─────────┐ ① 이벤트 발생
│  View   │──────────────────┐
│(Compose)│                  │
└────┬────┘                  │
     │ ⑧ UI 상태 업데이트     │
     │    (Recomposition)    │
     ▼                       │
┌──────────┐                 ▼
│StateFlow │           ┌──────────┐
│ (State)  │←──────────│ViewModel │
└──────────┘ ⑦ 결과 전달 └────┬─────┘
                            │ ② 로직 실행
                            │    (낙관적 업데이트)
                            ▼
                       ┌──────────┐
                       │ UseCase  │
                       └────┬─────┘
                            │ ③ 데이터 요청
                            ▼
                       ┌──────────────┐
                       │  Repository  │
                       └──┬────┬───┬──┘
                          │    │   │
        ④ API 호출         │    │   │ Cache 업데이트
                          │    │   │
         ┌────────────────┴┐  ┌▼───▼─────┐
         │   ApiService    │  │  Cache   │
         │   (Retrofit)    │  │ (Memory) │
         └────────┬────────┘  └──────────┘
                  │
         ⑤ 응답 데이터 전달
                  │
                  ▼
            ┌──────────┐
            │   WAS    │
            │ (Cloud   │
            │   Run)   │
            └──────────┘
```

### 디렉토리 구조

```
app/src/main/java/com/example/mynewsmobileappfe/
├── NewsApp.kt                          # Application 진입점 (@HiltAndroidApp)
├── MainActivity.kt                     # 메인 Activity
│
├── core/                               # 공통 기능 모듈
│   ├── common/                         # 공통 유틸리티
│   │   └── Resource.kt
│   ├── database/                       # Room Database
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   └── HighlightDao.kt
│   │   └── entity/
│   │       └── Highlight.kt
│   ├── jwt/                            # JWT 토큰 관리
│   │   └── TokenManager.kt
│   ├── navigation/                     # 네비게이션
│   │   ├── NavGraph.kt
│   │   ├── Screen.kt
│   │   ├── MainScreen.kt
│   │   └── BottomNavBar.kt
│   ├── network/                        # 네트워크 설정
│   │   ├── di/
│   │   │   ├── ApiNetworkModule.kt     # Retrofit, OkHttp 설정
│   │   │   ├── ApiServiceModule.kt     # ApiService 제공
│   │   │   └── TokenRefreshNetworkModule.kt
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt      # JWT 자동 주입
│   │       └── TokenAuthenticator.kt   # 401 응답 시 토큰 갱신
│   └── ui/                             # UI 테마
│       └── theme/
│           ├── Color.kt
│           ├── Theme.kt
│           └── Type.kt
│
└── feature/                            # 기능별 모듈 (Multi-Module)
    ├── auth/                           # 인증 (로그인/회원가입)
    │   ├── data/
    │   │   ├── remote/
    │   │   │   ├── api/
    │   │   │   │   ├── AuthApiService.kt
    │   │   │   │   └── TokenRefreshApiService.kt
    │   │   │   └── dto/
    │   │   │       ├── LoginRequest.kt
    │   │   │       ├── SignUpRequest.kt
    │   │   │       └── TokenResponse.kt
    │   │   └── repository/
    │   │       └── AuthRepositoryImpl.kt
    │   ├── domain/
    │   │   └── repository/
    │   │       └── AuthRepository.kt
    │   ├── ui/
    │   │   ├── view/
    │   │   │   ├── LoginScreen.kt
    │   │   │   └── SignUpScreen.kt
    │   │   ├── viewmodel/
    │   │   │   └── AuthViewModel.kt
    │   │   ├── AuthContract.kt
    │   │   └── validation/
    │   │       └── SignUpValidator.kt
    │   └── di/
    │       └── AuthModule.kt
    │
    ├── news/                           # 뉴스 피드
    │   ├── cache/                      # 메모리 캐싱
    │   │   ├── ArticleCache.kt
    │   │   ├── ReactionCache.kt
    │   │   └── BookmarkCache.kt
    │   ├── data/
    │   │   ├── local/
    │   │   │   └── UserActionStore.kt  # DataStore
    │   │   ├── remote/
    │   │   │   ├── api/
    │   │   │   │   └── ArticleApiService.kt
    │   │   │   └── dto/
    │   │   │       ├── ArticleResponse.kt
    │   │   │       └── ReactionRequest.kt
    │   │   └── repository/
    │   │       ├── ArticleRepositoryImpl.kt
    │   │       └── HighlightRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── ReactionType.kt
    │   │   │   └── Section.kt
    │   │   ├── repository/
    │   │   │   ├── ArticleRepository.kt
    │   │   │   └── HighlightRepository.kt
    │   │   └── usecase/
    │   │       └── ArticleActionManager.kt
    │   ├── nfc/                        # NFC 기능
    │   │   └── LinkHceService.kt
    │   ├── ui/
    │   │   ├── HomeViewModel.kt
    │   │   ├── ArticleDetailState.kt
    │   │   └── BookmarkEvent.kt
    │   └── di/
    │       └── ArticleModule.kt
    │
    ├── bookmark/                       # 북마크
    │   ├── data/
    │   ├── domain/
    │   ├── ui/
    │   │   ├── BookmarkScreen.kt
    │   │   └── BookmarkViewModel.kt
    │   └── di/
    │
    └── profile/                        # 프로필
        ├── data/
        ├── domain/
        ├── ui/
        │   └── ProfileScreen.kt
        └── di/
```

---

## 🎨 주요 화면

### 홈 화면 (카테고리별 뉴스)
- 상단: 7초마다 갱신되는 추천 뉴스
- 하단: 카테고리별 최신 뉴스 목록
- 각 기사마다 좋아요/싫어요 버튼

### 기사 상세 화면
- 뉴스 본문 및 이미지
- 좋아요/싫어요 총합
- 북마크 버튼
- 하이라이트 기능
- NFC 공유 버튼

### 북마크 화면
- 내가 저장한 기사 목록
- 하이라이트한 기사 모아보기

### 프로필 화면
- 사용자 정보
- 회원 탈퇴
- 로그아웃

---

## 🚦 시작하기

### 사전 요구사항

- Android Studio Ladybug | 2024.2.1 이상
- JDK 11 이상
- Android SDK 24 (Android 7.0) 이상
- NFC 지원 안드로이드 기기 (NFC 공유 기능 사용 시)

### 설치 및 실행

1. **레포지토리 클론**
```bash
git clone https://github.com/Samsung-SCSA-25th-Team2/MyNewsMobileAppFE.git
cd MyNewsMobileAppFE
```

2. **Android Studio에서 프로젝트 열기**
```
File > Open > 프로젝트 폴더 선택
```

3. **Gradle Sync**
```
Android Studio가 자동으로 의존성을 다운로드합니다.
```

4. **API 서버 설정**

`app/src/main/java/com/example/mynewsmobileappfe/core/network/di/NetworkConfig.kt` 파일에서 서버 URL을 설정하세요.

```kotlin
const val BASE_URL = "https://your-api-server.com/"
```

5. **앱 실행**
```
Run > Run 'app' (Shift + F10)
```

### 빌드

**Debug APK 생성**
```bash
./gradlew assembleDebug
```

**Release APK 생성**
```bash
./gradlew assembleRelease
```

---

## 📱 최소 요구사항

- **minSdk**: 24 (Android 7.0 Nougat)
- **targetSdk**: 36 (Android 15)
- **compileSdk**: 36

---

## 🧪 테스트

```bash
# Unit Test 실행
./gradlew test

# Android Instrumented Test 실행
./gradlew connectedAndroidTest
```

---

## 🔐 주요 보안 기능

- **JWT 토큰 기반 인증**: Access Token + Refresh Token
- **자동 토큰 갱신**: 401 응답 시 Authenticator가 자동으로 토큰 갱신
- **DataStore 암호화**: 민감한 정보는 DataStore에 안전하게 저장

---

## 📚 참고 자료

- [Android Developers - Modern Android Development](https://developer.android.com/modern-android-development)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt - Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [NFC Basics](https://developer.android.com/develop/connectivity/nfc/nfc)

---

## 👥 팀원

- 심현우 프로 
- 윤정수 프로
- 이미르 프로

---

## 📄 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.

---

## 🙏 감사합니다

**마이 뉴스**는 삼성 SDS SCSA 25기 팀 프로젝트로 개발되었습니다.

현대적인 안드로이드 개발 기술 스택(Kotlin, Jetpack Compose, MVVM, Clean Architecture, Hilt)을 학습하고 적용하며, 실무에 가까운 개발 경험을 쌓을 수 있었습니다.

---

<div align="center">
  <sub>Built with ❤️ by Team 2</sub>
</div>