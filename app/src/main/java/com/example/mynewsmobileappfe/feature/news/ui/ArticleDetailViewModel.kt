package com.example.mynewsmobileappfe.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.usecase.ArticleActionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 기사 상세 화면 ViewModel
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articleActionManager: ArticleActionManager
) : ViewModel() {

    private val _articleState = MutableStateFlow<ArticleDetailState>(ArticleDetailState.Idle)
    val articleState: StateFlow<ArticleDetailState> = _articleState.asStateFlow()

    // 사용자의 현재 반응 (좋아요/싫어요/없음)
    private val _userReaction = MutableStateFlow<ReactionType>(ReactionType.NONE)
    val userReaction: StateFlow<ReactionType> = _userReaction.asStateFlow()

    // 북마크 토글 결과 이벤트
    private val _bookmarkEvent = MutableStateFlow<BookmarkEvent>(BookmarkEvent.Idle)
    val bookmarkEvent: StateFlow<BookmarkEvent> = _bookmarkEvent.asStateFlow()

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
    }

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

            // 서버에서 받은 userReaction을 ReactionCache에 저장
            ReactionCache.setReactionFromString(articleId, article.userReaction)

            _userReaction.value = ReactionCache.getReaction(articleId)
        } else {
            _articleState.value = ArticleDetailState.Error("기사를 찾을 수 없습니다.")
        }
    }

    /**
     * 기사 반응 (좋아요/싫어요) - Optimistic Update
     */
    fun reactToArticle(articleId: Long, reactionType: ReactionType) {
        val currentReaction = _userReaction.value

        // ArticleActionManager를 통해 Optimistic Update 수행
        articleActionManager.reactToArticle(
            articleId = articleId,
            currentReaction = currentReaction,
            newReaction = reactionType,
            scope = viewModelScope,
            onError = { errorMessage ->
                // 에러 시 사용자 반응도 롤백
                _userReaction.value = currentReaction
            }
        )

        // 즉시 사용자 반응 상태 업데이트
        _userReaction.value = reactionType
    }

    /**
     * 북마크 토글 - Optimistic Update
     */
    fun toggleBookmark(articleId: Long, isCurrentlyBookmarked: Boolean) {
        // ArticleActionManager를 통해 Optimistic Update 수행
        articleActionManager.toggleBookmark(
            articleId = articleId,
            isCurrentlyBookmarked = isCurrentlyBookmarked,
            scope = viewModelScope,
            onError = { errorMessage ->
                _bookmarkEvent.value = BookmarkEvent.Error(errorMessage)
            }
        )

        // 즉시 이벤트 발행 (UI 피드백용)
        _bookmarkEvent.value = BookmarkEvent.Success(!isCurrentlyBookmarked)
    }

    fun resetBookmarkEvent() {
        _bookmarkEvent.value = BookmarkEvent.Idle
    }
}
