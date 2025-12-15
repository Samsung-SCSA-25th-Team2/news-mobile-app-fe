package com.example.mynewsmobileappfe.feature.news.ui

/**
 * 북마크 토글 이벤트
 */
sealed class BookmarkEvent {
    object Idle : BookmarkEvent()
    object Loading : BookmarkEvent()
    data class Success(val isBookmarked: Boolean) : BookmarkEvent()
    data class Error(val message: String) : BookmarkEvent()
}
