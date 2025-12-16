package com.example.mynewsmobileappfe.feature.bookmark.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import com.example.mynewsmobileappfe.feature.news.cache.ArticleCache
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
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
 * BookmarkViewModel - 동료 개발자 구현 가이드
 * ========================================
 *
 * [사용 가능한 Repository 메서드]
 *
 * 1. bookmarkRepository.getMyBookmarks(page, size): Flow<Resource<PageResponse<ArticleResponse>>>
 *    - 내 북마크 목록 조회 (최신 북마크 순)
 *    - 페이지네이션 지원
 *
 * 2. bookmarkRepository.addBookmark(articleId): Flow<Resource<Unit>>
 *    - 북마크 추가 (이미 있으면 무시)
 *
 * 3. bookmarkRepository.removeBookmark(articleId): Flow<Resource<Unit>>
 *    - 북마크 삭제 (없어도 성공)
 *
 * [구현 예시]
 * ```
 * fun loadBookmarks() {
 *     bookmarkRepository.getMyBookmarks(page = currentPage, size = 20)
 *         .onEach { result ->
 *             _bookmarksState.value = when (result) {
 *                 is Resource.Loading -> BookmarkState.Loading
 *                 is Resource.Success -> {
 *                     val pageData = result.data!!
 *                     isLastPage = pageData.last
 *                     BookmarkState.Success(pageData.content)
 *                 }
 *                 is Resource.Error -> BookmarkState.Error(result.message ?: "오류")
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 *
 * fun removeBookmark(articleId: Long) {
 *     bookmarkRepository.removeBookmark(articleId)
 *         .onEach { result ->
 *             if (result is Resource.Success) {
 *                 // 로컬 리스트에서 제거
 *                 _bookmarks.value = _bookmarks.value.filter { it.articleId != articleId }
 *             }
 *         }
 *         .launchIn(viewModelScope)
 * }
 * ```
 *
 * [화면 구성]
 * - 빈 상태: "북마크한 기사가 없습니다" 메시지
 * - 기사 목록: HomeScreen의 ArticleItem 재사용
 * - 스와이프하여 삭제 (SwipeToDismiss)
 * - Pull-to-Refresh
 */
@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    // TODO: 북마크 목록 상태
    private val _bookmarksState = MutableStateFlow<BookmarkState>(BookmarkState.Idle)
    val bookmarksState: StateFlow<BookmarkState> = _bookmarksState.asStateFlow()

    // TODO: 북마크 목록 (스와이프 삭제 등을 위해 별도 관리)
    private val _bookmarks = MutableStateFlow<List<ArticleResponse>>(emptyList())
    val bookmarks: StateFlow<List<ArticleResponse>> = _bookmarks.asStateFlow()

    init {
        loadBookmarks()

        // ArticleCache 변경 사항 구독하여 실시간 반영
        ArticleCache.articles
            .onEach { articlesMap ->
                Log.d("BookmarkViewModel", "ArticleCache updated. Total articles: ${articlesMap.size}")
                val bookmarkedFromCache = articlesMap.values.filter { it.bookmarked }
                Log.d("BookmarkViewModel", "Bookmarked articles in cache: ${bookmarkedFromCache.size}")

                // 로딩 상태일 때 초기 빈 캐시로 상태를 덮어쓰지 않는다.
                if (bookmarkedFromCache.isEmpty()) {
                    if (_bookmarksState.value is BookmarkState.Loading) {
                        Log.d("BookmarkViewModel", "Bookmarks empty but loading state, skipping")
                        return@onEach
                    }
                    Log.d("BookmarkViewModel", "No bookmarks in cache, setting Empty state")
                    _bookmarks.value = emptyList()
                    _bookmarksState.value = BookmarkState.Empty
                    return@onEach
                }

                val existingOrder = _bookmarks.value.map { it.articleId }
                val bookmarkedMap = bookmarkedFromCache.associateBy { it.articleId }
                val updatedExisting = existingOrder.mapNotNull { bookmarkedMap[it] }
                val newOnes = bookmarkedFromCache.filter { it.articleId !in existingOrder }
                val merged = newOnes + updatedExisting

                Log.d("BookmarkViewModel", "Updated bookmarks: ${merged.size} (${newOnes.size} new, ${updatedExisting.size} existing)")
                _bookmarks.value = merged
                _bookmarksState.value = BookmarkState.Success(merged)
            }
            .launchIn(viewModelScope)
    }

    // TODO: 북마크 목록 로드
    fun loadBookmarks() {
        bookmarkRepository.getMyBookmarks(page = 0, size = 20)
            .onEach { result ->
                _bookmarksState.value = when (result) {
                    is Resource.Loading -> BookmarkState.Loading
                    is Resource.Success -> {
                        val bookmarks = result.data!!.content
                        _bookmarks.value = bookmarks

                        // ArticleCache에 북마크 기사들 저장
                        ArticleCache.putArticles(bookmarks)

                        // 서버에서 받은 userReaction을 ReactionCache에 저장
                        ReactionCache.setReactionsFromArticles(bookmarks)

                        if (bookmarks.isEmpty()) BookmarkState.Empty
                        else BookmarkState.Success(bookmarks)
                    }
                    is Resource.Error -> BookmarkState.Error(result.message ?: "북마크를 불러오는데 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    // TODO: 새로고침
    fun refresh() {
        loadBookmarks()
    }

    // TODO: 더 보기 (페이지네이션)
    fun loadMore() {
        // 단순 예제에서는 첫 페이지만 사용
    }

    // TODO: 북마크 삭제
    fun removeBookmark(articleId: Long) {
        bookmarkRepository.removeBookmark(articleId)
            .onEach { result ->
                if (result is Resource.Success) {
                    val updated = _bookmarks.value.filter { it.articleId != articleId }
                    _bookmarks.value = updated
                    _bookmarksState.value = if (updated.isEmpty()) BookmarkState.Empty else BookmarkState.Success(updated)
                } else if (result is Resource.Error) {
                    _bookmarksState.value = BookmarkState.Error(result.message ?: "북마크 삭제에 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }
}

/**
 * 북마크 화면 상태 sealed class
 */
sealed class BookmarkState {
    object Idle : BookmarkState()
    object Loading : BookmarkState()
    object Empty : BookmarkState()
    data class Success(val bookmarks: List<ArticleResponse>) : BookmarkState()
    data class Error(val message: String) : BookmarkState()
}
