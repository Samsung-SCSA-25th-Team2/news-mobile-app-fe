package com.example.mynewsmobileappfe.feature.bookmark.domain.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {

    /**
     * 북마크 추가
     */
    fun addBookmark(articleId: Long): Flow<Resource<Unit>>

    /**
     * 북마크 삭제
     */
    fun removeBookmark(articleId: Long): Flow<Resource<Unit>>

    /**
     * 내 북마크 목록 조회
     */
    fun getMyBookmarks(page: Int = 0, size: Int = 20): Flow<Resource<PageResponse<ArticleResponse>>>
}