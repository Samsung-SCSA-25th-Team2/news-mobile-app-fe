package com.example.mynewsmobileappfe.feature.news.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 북마크 상태 캐시
 *
 * - 기사별 북마크 true/false를 메모리에서 관리
 * - ReactionCache처럼 StateFlow로 전체 화면 동기화
 */
object BookmarkCache {
    private val _bookmarks = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val bookmarks: StateFlow<Map<Long, Boolean>> = _bookmarks.asStateFlow()

    /** null이면 "아직 로컬에서 확정된 값 없음" */
    fun getBookmarkedOrNull(articleId: Long): Boolean? = _bookmarks.value[articleId]

    fun setBookmarked(articleId: Long, bookmarked: Boolean) {
        val current = _bookmarks.value.toMutableMap()
        current[articleId] = bookmarked
        _bookmarks.value = current
    }

    fun clear() {
        _bookmarks.value = emptyMap()
    }
}
