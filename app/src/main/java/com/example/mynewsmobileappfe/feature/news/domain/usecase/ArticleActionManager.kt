package com.example.mynewsmobileappfe.feature.news.domain.usecase

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.repository.ArticleRepository
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기사 액션 관리자
 *
 * 좋아요/싫어요/북마크 액션을 Optimistic Update 방식으로 처리합니다.
 * - 즉시 로컬 상태 업데이트 (로딩 화면 없이)
 * - 백그라운드에서 서버 요청
 * - 실패 시 롤백 (선택적)
 */
@Singleton
class ArticleActionManager @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarkRepository: BookmarkRepository
) {

    /**
     * 기사 좋아요/싫어요 (Optimistic Update)
     *
     * @param articleId 기사 ID
     * @param currentReaction 현재 반응 상태
     * @param newReaction 새로운 반응
     * @param scope CoroutineScope
     * @param onError 에러 콜백 (선택)
     */
    fun reactToArticle(
        articleId: Long,
        currentReaction: ReactionType,
        newReaction: ReactionType,
        scope: CoroutineScope,
        onError: ((String) -> Unit)? = null
    ) {
        // 동일한 반응이면 불필요한 호출을 막는다.
        if (currentReaction == newReaction) return

        // 사용자 반응 상태를 즉시 반영
        ReactionCache.setReaction(articleId, newReaction)

        // 1. 즉시 로컬 상태 업데이트 (Optimistic)
        ArticleCache.updateArticle(articleId) { article ->
            var likes = article.likes
            var dislikes = article.dislikes

            // 이전 반응 취소
            when (currentReaction) {
                ReactionType.LIKE -> likes = (likes - 1).coerceAtLeast(0)
                ReactionType.DISLIKE -> dislikes = (dislikes - 1).coerceAtLeast(0)
                else -> {}
            }

            // 새 반응 적용
            when (newReaction) {
                ReactionType.LIKE -> likes += 1
                ReactionType.DISLIKE -> dislikes += 1
                else -> {}
            }

            // userReaction 필드도 함께 업데이트하여 ArticleCache에 상태 보존
            val userReactionString = when (newReaction) {
                ReactionType.LIKE -> "LIKE"
                ReactionType.DISLIKE -> "DISLIKE"
                ReactionType.NONE -> null
            }

            article.copy(
                likes = likes,
                dislikes = dislikes,
                userReaction = userReactionString
            )
        }

        // 2. 백그라운드에서 서버 요청
        articleRepository.reactToArticle(articleId, newReaction)
            .onEach { result ->
                when (result) {
                    is Resource.Error -> {
                        // 실패 시 롤백
                        ArticleCache.updateArticle(articleId) { article ->
                            var likes = article.likes
                            var dislikes = article.dislikes

                            // 새 반응 취소
                            when (newReaction) {
                                ReactionType.LIKE -> likes = (likes - 1).coerceAtLeast(0)
                                ReactionType.DISLIKE -> dislikes = (dislikes - 1).coerceAtLeast(0)
                                else -> {}
                            }

                            // 이전 반응 복원
                            when (currentReaction) {
                                ReactionType.LIKE -> likes += 1
                                ReactionType.DISLIKE -> dislikes += 1
                                else -> {}
                            }

                            // userReaction도 이전 상태로 복원
                            val userReactionString = when (currentReaction) {
                                ReactionType.LIKE -> "LIKE"
                                ReactionType.DISLIKE -> "DISLIKE"
                                ReactionType.NONE -> null
                            }

                            article.copy(
                                likes = likes,
                                dislikes = dislikes,
                                userReaction = userReactionString
                            )
                        }
                        ReactionCache.setReaction(articleId, currentReaction)
                        onError?.invoke(result.message ?: "반응 처리 실패")
                    }
                    else -> {}
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)
    }

    /**
     * 북마크 토글 (Optimistic Update)
     *
     * @param articleId 기사 ID
     * @param isCurrentlyBookmarked 현재 북마크 상태
     * @param scope CoroutineScope
     * @param onError 에러 콜백 (선택)
     */
    fun toggleBookmark(
        articleId: Long,
        isCurrentlyBookmarked: Boolean,
        scope: CoroutineScope,
        onError: ((String) -> Unit)? = null
    ) {
        android.util.Log.d("ArticleActionManager", "toggleBookmark called: articleId=$articleId, current=$isCurrentlyBookmarked")

        // 1. 즉시 로컬 상태 업데이트 (Optimistic)
        val newBookmarkState = !isCurrentlyBookmarked

        // 캐시에 기사가 있는지 확인
        val article = ArticleCache.getArticle(articleId)
        if (article != null) {
            android.util.Log.d("ArticleActionManager", "Article found in cache, updating bookmark state to $newBookmarkState")
            ArticleCache.updateArticle(articleId) { article ->
                article.copy(bookmarked = newBookmarkState)
            }
        } else {
            android.util.Log.w("ArticleActionManager", "Article NOT found in cache! articleId=$articleId")
        }

        // 2. 백그라운드에서 서버 요청
        val flow = if (isCurrentlyBookmarked) {
            bookmarkRepository.removeBookmark(articleId)
        } else {
            bookmarkRepository.addBookmark(articleId)
        }

        flow
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        android.util.Log.d("ArticleActionManager", "Bookmark server request succeeded")
                    }
                    is Resource.Error -> {
                        android.util.Log.e("ArticleActionManager", "Bookmark server request failed: ${result.message}")
                        // 실패 시 롤백
                        ArticleCache.updateArticle(articleId) { article ->
                            article.copy(bookmarked = isCurrentlyBookmarked)
                        }
                        onError?.invoke(result.message ?: "북마크 처리 실패")
                    }
                    else -> {}
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)
    }
}
