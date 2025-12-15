package com.example.mynewsmobileappfe.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.repository.ArticleRepository
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 기사 상세 화면 ViewModel
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _articleState = MutableStateFlow<ArticleDetailState>(ArticleDetailState.Idle)
    val articleState: StateFlow<ArticleDetailState> = _articleState.asStateFlow()

    // 사용자의 현재 반응 (좋아요/싫어요/없음)
    private val _userReaction = MutableStateFlow<ReactionType>(ReactionType.NONE)
    val userReaction: StateFlow<ReactionType> = _userReaction.asStateFlow()

    // 북마크 토글 결과 이벤트
    private val _bookmarkEvent = MutableStateFlow<BookmarkEvent>(BookmarkEvent.Idle)
    val bookmarkEvent: StateFlow<BookmarkEvent> = _bookmarkEvent.asStateFlow()

    /**
     * 기사 상세 정보 로드
     *
     * Note: 현재 API에 기사 상세 조회 엔드포인트가 없으므로,
     * ArticleCache를 통해 HomeScreen에서 전달받은 데이터를 사용합니다.
     */
    fun loadArticle(articleId: Long) {
        _articleState.value = ArticleDetailState.Loading

        // ArticleCache에서 기사 정보 가져오기
        val article = ArticleCache.getArticle(articleId)
        if (article != null) {
            _articleState.value = ArticleDetailState.Success(article)
            _userReaction.value = ReactionType.NONE
        } else {
            _articleState.value = ArticleDetailState.Error("기사를 찾을 수 없습니다.")
        }
    }

    /**
     * 기사 반응 (좋아요/싫어요)
     */
    fun reactToArticle(articleId: Long, reactionType: ReactionType) {
        articleRepository.reactToArticle(articleId, reactionType)
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        val current = (_articleState.value as? ArticleDetailState.Success)?.article
                        if (current != null) {
                            val updated = updateReactionCounts(
                                currentArticle = current,
                                previous = _userReaction.value,
                                next = reactionType
                            )
                            ArticleCache.putArticle(updated)
                            _articleState.value = ArticleDetailState.Success(updated)
                            _userReaction.value = reactionType
                        } else {
                            _userReaction.value = reactionType
                        }
                    }
                    is Resource.Error -> {
                        // 에러 처리 (필요시 토스트 메시지)
                    }
                    is Resource.Loading -> {
                        // 로딩 상태 (필요시 표시)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun toggleBookmark(articleId: Long, isCurrentlyBookmarked: Boolean) {
        val flow = if (isCurrentlyBookmarked) {
            bookmarkRepository.removeBookmark(articleId)
        } else {
            bookmarkRepository.addBookmark(articleId)
        }

        flow
            .onEach { result ->
                _bookmarkEvent.value = when (result) {
                    is Resource.Success -> {
                        val current = (_articleState.value as? ArticleDetailState.Success)?.article
                        if (current != null) {
                            val updated = current.copy(bookmarked = !isCurrentlyBookmarked)
                            ArticleCache.putArticle(updated)
                            _articleState.value = ArticleDetailState.Success(updated)
                        }
                        BookmarkEvent.Success(!isCurrentlyBookmarked)
                    }
                    is Resource.Error -> BookmarkEvent.Error(result.message ?: "북마크 처리에 실패했습니다.")
                    is Resource.Loading -> BookmarkEvent.Idle
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun resetBookmarkEvent() {
        _bookmarkEvent.value = BookmarkEvent.Idle
    }

    private fun updateReactionCounts(
        currentArticle: ArticleResponse,
        previous: ReactionType,
        next: ReactionType
    ): ArticleResponse {
        var likes = currentArticle.likes
        var dislikes = currentArticle.dislikes

        // 이전 반응 해제
        when (previous) {
            ReactionType.LIKE -> likes = (likes - 1).coerceAtLeast(0)
            ReactionType.DISLIKE -> dislikes = (dislikes - 1).coerceAtLeast(0)
            else -> {}
        }

        // 새로운 반응 적용
        when (next) {
            ReactionType.LIKE -> likes += 1
            ReactionType.DISLIKE -> dislikes += 1
            else -> {}
        }

        return currentArticle.copy(likes = likes, dislikes = dislikes)
    }
}

/**
 * 기사 상세 화면 상태
 */
sealed class ArticleDetailState {
    object Idle : ArticleDetailState()
    object Loading : ArticleDetailState()
    data class Success(val article: ArticleResponse) : ArticleDetailState()
    data class Error(val message: String) : ArticleDetailState()
}

sealed class BookmarkEvent {
    object Idle : BookmarkEvent()
    object Loading : BookmarkEvent()
    data class Success(val isBookmarked: Boolean) : BookmarkEvent()
    data class Error(val message: String) : BookmarkEvent()
}

/**
 * 기사 캐시 (HomeScreen에서 ArticleDetailScreen으로 데이터 전달용)
 *
 * Note: 이는 임시 솔루션입니다. 프로덕션에서는:
 * 1. 백엔드에 기사 상세 조회 API 추가
 * 2. 또는 Navigation Arguments로 필요한 데이터만 전달
 * 3. 또는 Shared ViewModel 사용
 */
object ArticleCache {
    private val cache = mutableMapOf<Long, ArticleResponse>()

    fun putArticle(article: ArticleResponse) {
        val existing = cache[article.articleId]
        cache[article.articleId] = merge(existing, article)
    }

    fun getArticle(articleId: Long): ArticleResponse? {
        return cache[articleId]
    }

    fun clear() {
        cache.clear()
    }

    private fun merge(old: ArticleResponse?, new: ArticleResponse): ArticleResponse {
        if (old == null) return new
        return new.copy(
            content = new.content ?: old.content,
            likes = new.likes,
            dislikes = new.dislikes,
            bookmarked = new.bookmarked
        )
    }
}
