package com.example.mynewsmobileappfe.feature.bookmark.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import com.example.mynewsmobileappfe.feature.news.cache.ReactionCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _bookmarksState = MutableStateFlow<BookmarkState>(BookmarkState.Idle)
    val bookmarksState: StateFlow<BookmarkState> = _bookmarksState.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<ArticleResponse>>(emptyList())
    val bookmarks: StateFlow<List<ArticleResponse>> = _bookmarks.asStateFlow()

    init {
        // ✅ 북마크 화면은 "서버 응답만"으로 구성 (로컬 캐시 기반 merge/구독 제거)
        loadBookmarks()
    }

    fun loadBookmarks() {
        bookmarkRepository.getMyBookmarks(page = 0, size = 20)
            .onEach { result ->
                _bookmarksState.value = when (result) {
                    is Resource.Loading -> {
                        BookmarkState.Loading
                    }
                    is Resource.Success -> {
                        val serverBookmarks = result.data?.content.orEmpty()

                        // ✅ 화면 데이터는 서버에서 받은 목록만 그대로 사용
                        _bookmarks.value = serverBookmarks

                        // (옵션) 서버에서 받은 userReaction을 캐시에 저장 (상세/다른 화면 동기화 목적)
                        ReactionCache.setReactionsFromArticles(serverBookmarks)

                        if (serverBookmarks.isEmpty()) BookmarkState.Empty
                        else BookmarkState.Success(serverBookmarks)
                    }
                    is Resource.Error -> {
                        BookmarkState.Error(result.message ?: "북마크를 불러오는데 실패했습니다.")
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun refresh() {
        loadBookmarks()
    }

    fun loadMore() {
        // 단순 예제에서는 첫 페이지만 사용
    }

    fun removeBookmark(articleId: Long) {
        bookmarkRepository.removeBookmark(articleId)
            .onEach { result ->
                if (result is Resource.Success) {
                    val updated = _bookmarks.value.filter { it.articleId != articleId }
                    _bookmarks.value = updated
                    _bookmarksState.value =
                        if (updated.isEmpty()) BookmarkState.Empty else BookmarkState.Success(updated)
                } else if (result is Resource.Error) {
                    _bookmarksState.value = BookmarkState.Error(result.message ?: "북마크 삭제에 실패했습니다.")
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }
}

sealed class BookmarkState {
    object Idle : BookmarkState()
    object Loading : BookmarkState()
    object Empty : BookmarkState()
    data class Success(val bookmarks: List<ArticleResponse>) : BookmarkState()
    data class Error(val message: String) : BookmarkState()
}
