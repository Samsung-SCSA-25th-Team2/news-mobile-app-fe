package com.example.mynewsmobileappfe.feature.bookmark.data.remote.api

import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookmarkApiService {

    /**
     * 북마크 추가
     * - 이미 북마크된 기사는 무시 (멱등성 보장)
     * @param articleId 북마크할 기사 ID
     */
    @POST("bookmarks/{articleId}")
    suspend fun addBookmark(
        @Path("articleId") articleId: Long
    ): Response<Unit>

    /**
     * 북마크 삭제
     * - 북마크가 존재하지 않아도 성공 응답 (멱등성 보장)
     * @param articleId 삭제할 북마크의 기사 ID
     */
    @DELETE("bookmarks/{articleId}")
    suspend fun removeBookmark(
        @Path("articleId") articleId: Long
    ): Response<Unit>

    /**
     * 내 북마크 목록 조회
     * - 최신 북마크 순으로 정렬
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (최대 100)
     */
    @GET("bookmarks/me")
    suspend fun getMyBookmarks(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<ArticleResponse>>
}
