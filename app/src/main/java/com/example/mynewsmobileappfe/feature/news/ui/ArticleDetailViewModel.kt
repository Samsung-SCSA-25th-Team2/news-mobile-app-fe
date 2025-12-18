package com.example.mynewsmobileappfe.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.database.entity.Highlight
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.news.cache.BookmarkCache
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.repository.HighlightRepository
import com.example.mynewsmobileappfe.feature.news.domain.usecase.ArticleActionManager
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject
import com.example.mynewsmobileappfe.feature.news.data.remote.api.ArticleApiService

/**
 * 기사 상세 화면 ViewModel
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articleActionManager: ArticleActionManager,
    private val highlightRepository: HighlightRepository,
    private val articleApi: ArticleApiService,
) : ViewModel() {

    private val _articleState = MutableStateFlow<ArticleDetailState>(ArticleDetailState.Idle)
    val articleState: StateFlow<ArticleDetailState> = _articleState.asStateFlow()

    private val _userReaction = MutableStateFlow<ReactionType>(ReactionType.NONE)
    val userReaction: StateFlow<ReactionType> = _userReaction.asStateFlow()

    private val _bookmarkEvent = MutableStateFlow<BookmarkEvent>(BookmarkEvent.Idle)
    val bookmarkEvent: StateFlow<BookmarkEvent> = _bookmarkEvent.asStateFlow()

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights.asStateFlow()

    // 좋아요/싫어요 연타 방지를 위한 마지막 반응 시간
    private var lastReactionTime = 0L
    private val reactionCooldownMs = 500L // 0.5초

    init {
        // ArticleCache 변경 사항 구독
        ArticleCache.articles
            .onEach { articlesMap ->
                val currentState = _articleState.value
                if (currentState is ArticleDetailState.Success) {
                    articlesMap[currentState.article.articleId]?.let { updatedArticle ->
                        _articleState.value = ArticleDetailState.Success(updatedArticle)
                    }
                }
            }
            .launchIn(viewModelScope)

        // 사용자 반응 캐시 변경 사항 구독
        ReactionCache.reactions
            .onEach { reactionMap ->
                val currentState = _articleState.value
                if (currentState is ArticleDetailState.Success) {
                    _userReaction.value = reactionMap[currentState.article.articleId] ?: ReactionType.NONE
                }
            }
            .launchIn(viewModelScope)

        // ✅ 북마크 캐시 변경 사항 구독 (상세 화면 즉시 동기화)
        BookmarkCache.bookmarks
            .onEach { bookmarkMap ->
                val currentState = _articleState.value
                if (currentState is ArticleDetailState.Success) {
                    val id = currentState.article.articleId
                    bookmarkMap[id]?.let { bookmarked ->
                        ArticleCache.updateArticle(id) { it.copy(bookmarked = bookmarked) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 기사 상세 정보 로드
     *
     * ✅ 전략:
     * 1) 캐시에 있으면 먼저 보여줌(빠른 UI)
     * 2) 이후 서버에서 기사 상세를 반드시 조회해서 최신화/빈캐시 대응
     * 3) 서버 실패해도 캐시가 있으면 에러로 덮지 않음 (NFC 진입 안정화 핵심)
     */
    fun loadArticle(articleId: Long) {
        _articleState.value = ArticleDetailState.Loading

        // 0) 하이라이트는 기사 유무와 무관하게 로드 가능 (articleId 기준)
        loadHighlights(articleId)

        // 1) 캐시 먼저 표시
        val cached = ArticleCache.getArticle(articleId)
        if (cached != null) {
            _articleState.value = ArticleDetailState.Success(cached)

            ReactionCache.setReactionFromString(articleId, cached.userReaction)
            _userReaction.value = ReactionCache.getReaction(articleId)
        }

        // 2) 서버에서 반드시 조회 (캐시가 비어있는 NFC 진입 케이스 대응)
        viewModelScope.launch {
            val remoteResult = fetchArticleRemoteWithRetry(articleId)

            if (remoteResult.isSuccessful && remoteResult.body() != null) {
                val article = remoteResult.body()!!

                // ✅ 1) 서버 응답을 캐시에 먼저 넣어서 merge(북마크/반응 정합성 유지)
                ArticleCache.putArticle(article)

                // ✅ 2) 캐시에서 "merge된 결과"를 다시 꺼내서 화면에 반영
                val merged = ArticleCache.getArticle(articleId) ?: article
                _articleState.value = ArticleDetailState.Success(merged)

                // ✅ userReaction 동기화도 merged 기준으로
                ReactionCache.setReactionFromString(articleId, merged.userReaction)
                _userReaction.value = ReactionCache.getReaction(articleId)

                return@launch
            }

            // 3) 서버 실패 처리
            //    - 캐시가 이미 있으면, 화면은 캐시로 유지하고 에러 토스트/로그만 남기는 편이 안정적
            val hasCache = cached != null
            val code = remoteResult.code()
            val msg = when (code) {
                404 -> "기사를 찾을 수 없습니다."
                401, 403 -> "로그인이 필요합니다."
                else -> "기사 로드 실패 (HTTP $code)"
            }

            android.util.Log.w("ArticleDetailViewModel", "loadArticle($articleId) failed: $msg")

            if (!hasCache) {
                _articleState.value = ArticleDetailState.Error(msg)
            }
            // hasCache면 그대로 Success 유지 (NFC 진입에서 “가끔 Error로 바뀌는” 문제 방지)
        }
    }

    /**
     * 서버 기사 상세 조회 (재시도 1회)
     */
    private suspend fun fetchArticleRemoteWithRetry(articleId: Long): Response<ArticleResponse> {
        // 1차 시도
        val first = runCatching { articleApi.getArticleById(articleId) }
        if (first.isSuccess) return first.getOrThrow()

        // 잠깐 쉬고 2차 시도 (앱 cold start 직후 네트워크 흔들림 완화)
        delay(200L)

        // 2차 시도
        return runCatching { articleApi.getArticleById(articleId) }
            .getOrElse { e ->
                android.util.Log.w("ArticleDetailViewModel", "network error: ${e.message}", e)
                // 네트워크 예외는 "가짜 Response"로 처리하기 애매해서,
                // 여기서는 500처럼 취급해 에러 메시지로 떨어뜨리자.
                // (혹은 sealed Result로 바꾸는 게 더 깔끔하지만 지금은 최소 수정)
                Response.error(500, okhttp3.ResponseBody.create(null, "network error"))
            }
    }

    private fun loadHighlights(articleId: Long) {
        highlightRepository.getHighlightsByArticleId(articleId)
            .onEach { highlights ->
                _highlights.value = highlights
            }
            .launchIn(viewModelScope)
    }

    fun addHighlight(
        articleId: Long,
        startIndex: Int,
        endIndex: Int,
        text: String,
        color: String
    ) {
        viewModelScope.launch {
            try {
                highlightRepository.addHighlight(
                    articleId = articleId,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    text = text,
                    color = color
                )
            } catch (e: Exception) {
                android.util.Log.e("ArticleDetailViewModel", "Failed to add highlight", e)
            }
        }
    }

    fun deleteHighlight(highlightId: Long) {
        viewModelScope.launch {
            try {
                highlightRepository.deleteHighlight(highlightId)
            } catch (e: Exception) {
                android.util.Log.e("ArticleDetailViewModel", "Failed to delete highlight", e)
            }
        }
    }

    fun reactToArticle(articleId: Long, reactionType: ReactionType) {
        // 연타 방지: 마지막 반응 후 0.5초가 지나지 않았으면 무시
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastReactionTime < reactionCooldownMs) {
            android.util.Log.d("ArticleDetailViewModel", "Reaction ignored due to cooldown")
            return
        }
        lastReactionTime = currentTime

        val currentReaction = _userReaction.value

        articleActionManager.reactToArticle(
            articleId = articleId,
            currentReaction = currentReaction,
            newReaction = reactionType,
            scope = viewModelScope,
            onError = { _ ->
                _userReaction.value = currentReaction
            }
        )

        _userReaction.value = reactionType
    }

    fun toggleBookmark(articleId: Long, isCurrentlyBookmarked: Boolean) {
        articleActionManager.toggleBookmark(
            articleId = articleId,
            isCurrentlyBookmarked = isCurrentlyBookmarked,
            scope = viewModelScope,
            onError = { errorMessage ->
                _bookmarkEvent.value = BookmarkEvent.Error(errorMessage)
            }
        )

        _bookmarkEvent.value = BookmarkEvent.Success(!isCurrentlyBookmarked)
    }

    fun resetBookmarkEvent() {
        _bookmarkEvent.value = BookmarkEvent.Idle
    }
}
