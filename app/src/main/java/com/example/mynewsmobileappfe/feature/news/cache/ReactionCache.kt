package com.example.mynewsmobileappfe.feature.news.cache

import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 사용자 반응 캐시
 *
 * 각 기사에 대한 사용자의 현재 반응 상태를 메모리에서 관리합니다.
 */
object ReactionCache {
    private val _reactions = MutableStateFlow<Map<Long, ReactionType>>(emptyMap())
    val reactions: StateFlow<Map<Long, ReactionType>> = _reactions.asStateFlow()

    fun getReaction(articleId: Long): ReactionType {
        return _reactions.value[articleId] ?: ReactionType.NONE
    }

    fun setReaction(articleId: Long, reactionType: ReactionType) {
        val current = _reactions.value.toMutableMap()
        if (reactionType == ReactionType.NONE) {
            current.remove(articleId)
        } else {
            current[articleId] = reactionType
        }
        _reactions.value = current
    }

    /**
     * 서버에서 받은 userReaction 문자열을 ReactionType으로 변환하여 저장
     */
    fun setReactionFromString(articleId: Long, userReaction: String?) {
        val reactionType = when (userReaction?.uppercase()) {
            "LIKE" -> ReactionType.LIKE
            "DISLIKE" -> ReactionType.DISLIKE
            else -> ReactionType.NONE
        }
        setReaction(articleId, reactionType)
    }

    /**
     * 여러 기사의 반응을 한 번에 저장 (기사 목록 로드 시)
     *
     * 중요: 서버 데이터가 있으면 서버를 신뢰하고,
     * 서버 데이터가 없으면 로컬 상태를 유지합니다.
     */
    fun setReactionsFromArticles(articles: List<ArticleResponse>) {
        val current = _reactions.value.toMutableMap()
        articles.forEach { article ->
            val reactionType = when (article.userReaction?.uppercase()) {
                "LIKE" -> ReactionType.LIKE
                "DISLIKE" -> ReactionType.DISLIKE
                else -> ReactionType.NONE
            }

            // 서버에서 명시적으로 LIKE 또는 DISLIKE를 반환한 경우에만 업데이트
            if (reactionType != ReactionType.NONE) {
                current[article.articleId] = reactionType
            }
            // userReaction이 null이면 로컬 상태 유지 (삭제하지 않음)
        }
        _reactions.value = current
    }

    fun clear() {
        _reactions.value = emptyMap()
    }
}