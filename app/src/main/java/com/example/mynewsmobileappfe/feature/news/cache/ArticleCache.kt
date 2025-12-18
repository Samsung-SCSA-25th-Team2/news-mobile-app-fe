package com.example.mynewsmobileappfe.feature.news.cache

import android.util.Log
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기사 캐시 (전역 상태 관리)
 *
 * StateFlow 기반으로 변경 사항을 자동으로 모든 화면에 반영합니다.
 * - 좋아요/싫어요/북마크 액션 시 즉시 로컬 상태 업데이트 (Optimistic Update)
 * - 모든 ViewModel이 articles StateFlow를 구독하여 실시간 동기화
 */
object ArticleCache {
    private val _articles = MutableStateFlow<Map<Long, ArticleResponse>>(emptyMap())
    val articles: StateFlow<Map<Long, ArticleResponse>> = _articles.asStateFlow()

    fun putArticle(article: ArticleResponse) {
        val current = _articles.value.toMutableMap()
        val existing = current[article.articleId]
        current[article.articleId] = merge(existing, article)
        _articles.value = current
    }

    fun putArticles(newArticles: List<ArticleResponse>) {
        val current = _articles.value.toMutableMap()
        newArticles.forEach { article ->
            val existing = current[article.articleId]
            val merged = merge(existing, article)

            Log.d("ArticleCache", "Merging article ${article.articleId}: " +
                "old=(bookmarked=${existing?.bookmarked}, userReaction=${existing?.userReaction}), " +
                "new=(bookmarked=${article.bookmarked}, userReaction=${article.userReaction}), " +
                "result=(bookmarked=${merged.bookmarked}, userReaction=${merged.userReaction})")

            current[article.articleId] = merged
        }
        _articles.value = current
    }

    fun getArticle(articleId: Long): ArticleResponse? {
        return _articles.value[articleId]
    }

    fun updateArticle(articleId: Long, update: (ArticleResponse) -> ArticleResponse) {
        val current = _articles.value.toMutableMap()
        current[articleId]?.let { article ->
            val updated = update(article)
            current[articleId] = updated
            Log.d("ArticleCache", "Article $articleId updated. bookmarked=${updated.bookmarked}, likes=${updated.likes}, dislikes=${updated.dislikes}")
            _articles.value = current
        } ?: run {
            Log.w("ArticleCache", "Failed to update article $articleId - not found in cache")
        }
    }

    fun clear() {
        _articles.value = emptyMap()
    }

    private fun merge(old: ArticleResponse?, new: ArticleResponse): ArticleResponse {
        if (old == null) {
            // 캐시에 처음 들어오는 경우라도 BookmarkCache에 값 있으면 덮어씀
            val overlay = BookmarkCache.getBookmarkedOrNull(new.articleId)
            return if (overlay != null) new.copy(bookmarked = overlay) else new
        }

        val bookmarkOverlay = BookmarkCache.getBookmarkedOrNull(new.articleId)

        return new.copy(
            content = new.content ?: old.content,
            likes = new.likes,
            dislikes = new.dislikes,

            // ✅ 북마크: 로컬(BookmarkCache) 값이 있으면 그걸 최우선으로
            bookmarked = bookmarkOverlay ?: (old.bookmarked || new.bookmarked),

            // ✅ 반응: 서버 null이면 로컬 값 유지
            userReaction = new.userReaction ?: old.userReaction
        )
    }
}