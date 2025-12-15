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
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

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
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private var currentPage = 0
    private var isLastPage = false
    private val articlesList = mutableListOf<ArticleResponse>()

    // TODO: 현재 선택된 섹션
    private val _selectedSection = MutableStateFlow(Section.POLITICS)
    val selectedSection: StateFlow<Section> = _selectedSection.asStateFlow()

    // TODO: 기사 목록 상태
    private val _articlesState = MutableStateFlow<HomeState>(HomeState.Idle)
    val articlesState: StateFlow<HomeState> = _articlesState.asStateFlow()

    // TODO: 랜덤 기사 상태 (홈 상단 배너용)
    private val _randomArticle = MutableStateFlow<ArticleRandomResponse?>(null)
    val randomArticle: StateFlow<ArticleRandomResponse?> = _randomArticle.asStateFlow()

    // TODO: 섹션 선택 시 기사 로드
    fun selectSection(section: Section) {
        android.util.Log.d("HomeViewModel", "selectSection called with: $section")
        android.util.Log.d("HomeViewModel", "ViewModel instance: ${this.hashCode()}")

        // 같은 섹션이면 불필요한 재호출을 막는다.
        if (_selectedSection.value == section && articlesList.isNotEmpty()) {
            return
        }

        _selectedSection.value = section
        loadArticles(section)
        loadRandomArticle(section)
    }

    // TODO: 기사 목록 로드
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
                        isLastPage = data.last
                        articlesList.clear()
                        articlesList.addAll(data.content)
                        HomeState.Success(data.copy(content = articlesList.toList()))
                    }
                    is Resource.Error -> HomeState.Error(result.message ?: "기사를 불러오는데 실패했습니다.")
                }
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
                        currentPage = nextPage
                        isLastPage = data.last
                        articlesList.addAll(data.content)
                        _articlesState.value = HomeState.Success(
                            data.copy(
                                content = articlesList.toList(),
                                page = currentPage
                            )
                        )
                    }
                    is Resource.Error -> _articlesState.value = HomeState.Error(result.message ?: "기사를 불러오는데 실패했습니다.")
                    is Resource.Loading -> _articlesState.value = HomeState.Loading
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 좋아요/싫어요
    fun reactToArticle(articleId: Long, reactionType: ReactionType) {
        articleRepository.reactToArticle(articleId, reactionType)
            .onEach { result ->
                if (result is Resource.Success) {
                    // 간단히 최신 데이터로 새로고침
                    refresh()
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 북마크 토글
    fun toggleBookmark(articleId: Long, isCurrentlyBookmarked: Boolean) {
        val flow = if (isCurrentlyBookmarked) {
            bookmarkRepository.removeBookmark(articleId)
        } else {
            bookmarkRepository.addBookmark(articleId)
        }

        flow
            .onEach { result ->
                if (result is Resource.Success) {
                    // 북마크 상태 변경 후에도 목록은 유지
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    private fun loadRandomArticle(section: Section) {
        articleRepository.getRandomArticle(section)
            .onEach { result ->
                if (result is Resource.Success) {
                    _randomArticle.value = result.data
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
