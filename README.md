## 주요 기술 스택 및 라이브러리

### 핵심 아키텍처
- **MVVM + Clean Architecture**  
  UI, Domain, Data 레이어 분리

- **Unidirectional Data Flow**  
  StateFlow를 통한 단방향 데이터 흐름

- **Repository Pattern**  
  데이터 소스 추상화

---

## 의존성 주입
- **Hilt**
    - `@HiltAndroidApp`
    - `@AndroidEntryPoint`
    - `@Inject`

---

## 비동기 처리
- **Kotlin Coroutines**
    - `suspend` 함수
    - `viewModelScope`

- **Flow & StateFlow**
    - 반응형 데이터 스트림

---

## 네트워킹
- **Retrofit**
    - REST API 통신

- **OkHttp**
    - 인터셉터를 통한 JWT 자동 추가

- **Gson / Moshi**
    - JSON 직렬화

---

## 로컬 데이터
- **Room Database**
    - 오프라인 캐싱

- **DataStore**
    - 토큰 저장

---

## UI
- **Jetpack Compose**
    - 선언형 UI

- **Navigation Compose**
    - 화면 전환

- **Coil**
    - 이미지 로딩
