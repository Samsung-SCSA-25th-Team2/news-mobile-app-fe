package com.example.mynewsmobileappfe.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleRandomResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.model.Section
import com.example.mynewsmobileappfe.feature.news.domain.repository.ArticleRepository
import com.example.mynewsmobileappfe.feature.news.domain.usecase.ArticleActionManager
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
import com.example.mynewsmobileappfe.feature.news.data.local.UserActionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.jvm.Volatile

/**
 * ========================================
 * HomeViewModel - 동료 개발자 구현 가이드
 * ========================================
 *
 * [사용 가능한 Repository 메서드]
 *
 * === ArticleRepository ===
 * 1. articleRepository.getArticles(section, page, size): Flow<Resource<PageResponse<ArticleResponse>>>
 *    - 섹션별 기사 목록 조회 (페이지네이션)
 *    - section: Section.POLITICS, Section.ECONOMY, Section.SOCIAL, Section.TECHNOLOGY
 *
 * 2. articleRepository.getRandomArticle(section, date): Flow<Resource<ArticleRandomResponse>>
 *    - 섹션별 랜덤 기사 1개 조회
 *    - date: null이면 오늘 날짜, "2025-12-14" 형식
 *
 * 3. articleRepository.reactToArticle(articleId, reactionType): Flow<Resource<Unit>>
 *    - 기사 좋아요/싫어요
 *    - reactionType: ReactionType.LIKE, ReactionType.DISLIKE, ReactionType.NONE
 *
 * === BookmarkRepository ===
 * 4. bookmarkRepository.addBookmark(articleId): Flow<Resource<Unit>>
 *    - 북마크 추가
 *
 * 5. bookmarkRepository.removeBookmark(articleId): Flow<Resource<Unit>>
 *    - 북마크 삭제
 *
 * [구현 예시 - 기사 목록 조회]
 * ```
 * fun loadArticles(section: Section) {
 *     articleRepository.getArticles(section, page = 0, size = 20)
 *         .onEach { result ->
 *             _articlesState.value = when (result) {
 *                 is Resource.Loading -> HomeState.Loading
 *                 is Resource.Success -> HomeState.Success(result.data!!)
 *                 is Resource.Error -> HomeState.Error(result.message ?: "오류")
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 * ```
 *
 * [구현 예시 - 페이지네이션]
 * ```
 * private var currentPage = 0
 * private var isLastPage = false
 * private val articlesList = mutableListOf<ArticleResponse>()
 *
 * fun loadMore() {
 *     if (isLastPage) return
 *     currentPage++
 *     // 추가 로드 후 articlesList에 append
 * }
 * ```
 *
 * [화면 구성]
 * - 상단: 섹션 탭 (정치, 경제, 사회, IT/기술)
 * - 중앙: 기사 목록 (LazyColumn)
 * - 각 아이템: 썸네일, 제목, 출처, 날짜, 좋아요/싫어요 수
 * - Pull-to-Refresh 지원
 * - 무한 스크롤 (페이지네이션)
 *
 * [ArticleResponse 필드]
 * - articleId: Long
 * - section: String
 * - title: String
 * - content: String? (목록에선 null일 수 있음)
 * - url: String (기사 원문 링크)
 * - thumbnailUrl: String?
 * - source: String
 * - publisher: String
 * - publishedAt: String (ISO 8601)
 * - likes: Int
 * - dislikes: Int
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val articleActionManager: ArticleActionManager,
    private val userActionStore: UserActionStore
) : ViewModel() {

    private var currentPage = 0
    private var isLastPage = false
    private val articlesList = mutableListOf<ArticleResponse>()
    @Volatile
    private var latestUserActions: UserActionStore.Snapshot = UserActionStore.Snapshot()

    // 섹션별 기사 목록 캐시 (빠른 섹션 전환을 위해)
    private val sectionCache = mutableMapOf<Section, PageResponse<ArticleResponse>>()

    // 섹션별 랜덤 기사 캐시 (즉시 표시를 위해)
    private val randomArticleCache = mutableMapOf<Section, ArticleRandomResponse>()

    // TODO: 현재 선택된 섹션
    private val _selectedSection = MutableStateFlow(Section.POLITICS)
    val selectedSection: StateFlow<Section> = _selectedSection.asStateFlow()

    // TODO: 기사 목록 상태
    private val _articlesState = MutableStateFlow<HomeState>(HomeState.Idle)
    val articlesState: StateFlow<HomeState> = _articlesState.asStateFlow()

    // TODO: 랜덤 기사 상태 (홈 상단 배너용)
    private val _randomArticle = MutableStateFlow<ArticleRandomResponse?>(null)
    val randomArticle: StateFlow<ArticleRandomResponse?> = _randomArticle.asStateFlow()

    init {
        // ArticleCache 변경 사항 구독하여 실시간 반영
        ArticleCache.articles
            .onEach { articlesMap ->
                val currentState = _articlesState.value
                if (currentState is HomeState.Success) {
                    // 현재 목록의 기사들을 ArticleCache의 최신 상태로 업데이트
                    val updatedContent = currentState.data.content.map { article ->
                        articlesMap[article.articleId] ?: article
                    }
                    _articlesState.value = HomeState.Success(
                        currentState.data.copy(content = updatedContent)
                    )
                }
            }
            .launchIn(viewModelScope)

        // 로컬에 저장된 액션(좋아요/싫어요/북마크)을 복원
        userActionStore.snapshotFlow
            .onEach { snapshot ->
                latestUserActions = snapshot
                // ReactionCache 복원
                snapshot.reactions.forEach { (articleId, reaction) ->
                    ReactionCache.setReaction(articleId, reaction)
                    ArticleCache.updateArticle(articleId) { article ->
                        article.copy(
                            userReaction = when (reaction) {
                                ReactionType.LIKE -> "LIKE"
                                ReactionType.DISLIKE -> "DISLIKE"
                                ReactionType.NONE -> null
                            }
                        )
                    }
                }

                // 북마크 복원
                snapshot.bookmarkedIds.forEach { articleId ->
                    ArticleCache.updateArticle(articleId) { article ->
                        article.copy(bookmarked = true)
                    }
                }
            }
            .launchIn(viewModelScope)

        // 모든 섹션의 기사와 랜덤 기사를 백그라운드에서 미리 로드
        android.util.Log.d("HomeViewModel", "Preloading all sections...")
        Section.entries.forEach { section ->
            android.util.Log.d("HomeViewModel", "Preloading section: $section")
            loadArticlesInBackground(section)
            preloadRandomArticle(section)
        }
    }

    // TODO: 섹션 선택 시 기사 로드
    fun selectSection(section: Section) {
        android.util.Log.d("HomeViewModel", "selectSection called with: $section")
        android.util.Log.d("HomeViewModel", "ViewModel instance: ${this.hashCode()}")

        val isSameSection = _selectedSection.value == section
        _selectedSection.value = section

        // 캐시된 랜덤 기사 즉시 표시
        randomArticleCache[section]?.let {
            android.util.Log.d("HomeViewModel", "Using cached random article for section: $section")
            _randomArticle.value = it
        }

        // 같은 섹션이면 기사 목록은 재로드하지 않음
        if (isSameSection && articlesList.isNotEmpty()) {
            android.util.Log.d("HomeViewModel", "Same section, no action needed")
            return
        }

        // 캐시된 기사 목록 즉시 표시
        val cachedData = sectionCache[section]
        if (cachedData != null) {
            android.util.Log.d("HomeViewModel", "Using cached articles for section: $section")
            articlesList.clear()
            articlesList.addAll(cachedData.content)
            _articlesState.value = HomeState.Success(cachedData.copy(content = articlesList.toList()))
            currentPage = cachedData.page
            isLastPage = cachedData.last
        } else {
            // 캐시가 없으면 빈 상태로 시작 (곧 로드될 예정)
            android.util.Log.d("HomeViewModel", "No cache yet, showing empty state for section: $section")
            articlesList.clear()
            _articlesState.value = HomeState.Success(
                PageResponse(
                    content = emptyList(),
                    page = 0,
                    size = 20,
                    totalElements = 0,
                    totalPages = 0,
                    last = true
                )
            )
        }

        // 백그라운드에서 조용히 업데이트
        loadArticlesInBackground(section)
    }

    // TODO: 기사 목록 로드 (로딩 상태 표시)
    fun loadArticles(section: Section) {
        currentPage = 0
        isLastPage = false
        articlesList.clear()
        articleRepository.getArticles(section, page = currentPage, size = 20)
            .onEach { result ->
                _articlesState.value = when (result) {
                    is Resource.Loading -> HomeState.Loading
                    is Resource.Success -> {
                        val data = result.data!!
                        val enrichedContent = enrichWithPersistedState(data.content)
                        val enrichedData = data.copy(content = enrichedContent)
                        isLastPage = data.last
                        articlesList.clear()
                        articlesList.addAll(enrichedContent)

                        // ArticleCache에 기사들 저장
                        ArticleCache.putArticles(enrichedContent)

                        // 서버에서 받은 userReaction을 ReactionCache에 저장
                        ReactionCache.setReactionsFromArticles(enrichedContent)

                        // 섹션별 캐시에 저장
                        sectionCache[section] = enrichedData.copy(content = articlesList.toList())

                        HomeState.Success(enrichedData.copy(content = articlesList.toList()))
                    }
                    is Resource.Error -> HomeState.Error(result.message ?: "기사를 불러오는데 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // 백그라운드에서 조용히 기사 목록 로드 (로딩 상태 표시 안함)
    private fun loadArticlesInBackground(section: Section) {
        articleRepository.getArticles(section, page = 0, size = 20)
            .onEach { result ->
                if (result is Resource.Success) {
                    val data = result.data!!
                    val enrichedContent = enrichWithPersistedState(data.content)
                    val enrichedData = data.copy(content = enrichedContent)

                    // ArticleCache에 기사들 저장
                    ArticleCache.putArticles(enrichedContent)

                    // 서버에서 받은 userReaction을 ReactionCache에 저장
                    ReactionCache.setReactionsFromArticles(enrichedContent)

                    // 섹션별 캐시에 저장
                    sectionCache[section] = enrichedData

                    // 현재 선택된 섹션인 경우에만 공유 변수 및 UI 업데이트
                    if (_selectedSection.value == section) {
                        currentPage = 0
                        isLastPage = data.last
                        articlesList.clear()
                        articlesList.addAll(enrichedContent)
                        _articlesState.value = HomeState.Success(enrichedData.copy(content = articlesList.toList()))
                    }
                }
                // Error는 무시 (캐시된 데이터 유지)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 새로고침
    fun refresh() {
        loadArticles(_selectedSection.value)
    }

    // TODO: 더 보기 (페이지네이션)
    fun loadMore() {
        if (isLastPage) return
        val nextPage = currentPage + 1
        articleRepository.getArticles(_selectedSection.value, page = nextPage, size = 20)
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data!!
                        val enrichedContent = enrichWithPersistedState(data.content)
                        currentPage = nextPage
                        isLastPage = data.last
                        articlesList.addAll(enrichedContent)

                        // ArticleCache에 새 기사들 저장
                        ArticleCache.putArticles(enrichedContent)

                        // 서버에서 받은 userReaction을 ReactionCache에 저장
                        ReactionCache.setReactionsFromArticles(enrichedContent)

                        val updatedData = data.copy(
                            content = articlesList.toList(),
                            page = currentPage
                        )

                        // 섹션별 캐시 업데이트
                        sectionCache[_selectedSection.value] = updatedData

                        _articlesState.value = HomeState.Success(updatedData)
                    }
                    is Resource.Error -> _articlesState.value = HomeState.Error(result.message ?: "기사를 불러오는데 실패했습니다.")
                    is Resource.Loading -> { /* 로딩 스피너 표시 안함 - 기존 데이터 유지 */ }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 좋아요/싫어요 - Optimistic Update (로딩 없이 즉시 반영)
    fun toggleLike(articleId: Long) {
        val currentReaction = ReactionCache.getReaction(articleId)
        val newReaction = if (currentReaction == ReactionType.LIKE) {
            ReactionType.NONE
        } else {
            ReactionType.LIKE
        }
        articleActionManager.reactToArticle(
            articleId = articleId,
            currentReaction = currentReaction,
            newReaction = newReaction,
            scope = viewModelScope
        )
    }

    fun toggleDislike(articleId: Long) {
        val currentReaction = ReactionCache.getReaction(articleId)
        val newReaction = if (currentReaction == ReactionType.DISLIKE) {
            ReactionType.NONE
        } else {
            ReactionType.DISLIKE
        }
        articleActionManager.reactToArticle(
            articleId = articleId,
            currentReaction = currentReaction,
            newReaction = newReaction,
            scope = viewModelScope
        )
    }

    // TODO: 북마크 토글 - Optimistic Update (로딩 없이 즉시 반영)
    fun toggleBookmark(articleId: Long, isCurrentlyBookmarked: Boolean) {
        articleActionManager.toggleBookmark(
            articleId = articleId,
            isCurrentlyBookmarked = isCurrentlyBookmarked,
            scope = viewModelScope
        )
    }

    private fun reactionToString(reaction: ReactionType): String? {
        return when (reaction) {
            ReactionType.LIKE -> "LIKE"
            ReactionType.DISLIKE -> "DISLIKE"
            ReactionType.NONE -> null
        }
    }

    private fun enrichWithPersistedState(articles: List<ArticleResponse>): List<ArticleResponse> {
        val snapshot = latestUserActions

        return articles.map { article ->
            var enriched = article

            // 북마크 로컬 상태 반영
            if (snapshot.bookmarkedIds.contains(article.articleId)) {
                enriched = enriched.copy(bookmarked = true)
            }

            // 반응 로컬 상태 반영
            snapshot.reactions[article.articleId]?.let { reaction ->
                enriched = enriched.copy(userReaction = reactionToString(reaction))
            }

            enriched
        }
    }

    fun loadRandomArticle(section: Section) {
        articleRepository.getRandomArticle(section)
            .onEach { result ->
                if (result is Resource.Success) {
                    var randomArticle = result.data

                    // 로컬 저장소의 상태 반영
                    randomArticle?.let { article ->
                        val snapshot = userActionStore.getSnapshot()
                        if (snapshot.bookmarkedIds.contains(article.articleId)) {
                            randomArticle = article.copy(bookmarked = true)
                        }
                        snapshot.reactions[article.articleId]?.let { reaction ->
                            randomArticle = randomArticle?.copy(userReaction = reactionToString(reaction))
                        }
                    }

                    // 캐시에 저장
                    randomArticle?.let { adjusted ->
                        randomArticleCache[section] = adjusted

                        // 현재 선택된 섹션이면 즉시 표시
                        if (_selectedSection.value == section) {
                            _randomArticle.value = adjusted
                        }

                        // userReaction도 ReactionCache에 저장
                        ReactionCache.setReactionFromString(adjusted.articleId, adjusted.userReaction)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // 프리로드용 랜덤 기사 로드 (현재 섹션이 아니어도 캐시에 저장만)
    private fun preloadRandomArticle(section: Section) {
        articleRepository.getRandomArticle(section)
            .onEach { result ->
                if (result is Resource.Success) {
                    result.data?.let { randomArticle ->
                        val snapshot = userActionStore.getSnapshot()
                        val enriched = randomArticle.copy(
                            bookmarked = snapshot.bookmarkedIds.contains(randomArticle.articleId) || randomArticle.bookmarked,
                            userReaction = snapshot.reactions[randomArticle.articleId]?.let { reactionToString(it) } ?: randomArticle.userReaction
                        )

                        android.util.Log.d("HomeViewModel", "Cached random article for section: $section")
                        randomArticleCache[section] = enriched
                        ReactionCache.setReactionFromString(enriched.articleId, enriched.userReaction)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }
}

/**
 * 홈 화면 상태 sealed class
 */
sealed class HomeState {
    object Idle : HomeState()
    object Loading : HomeState()
    data class Success(val data: PageResponse<ArticleResponse>) : HomeState()
    data class Error(val message: String) : HomeState()
}
