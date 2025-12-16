package com.example.mynewsmobileappfe.feature.news.data.remote.api

import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleRandomResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ReactionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ArticleApiService {

    /**
     * 섹션별 기사 목록 조회
     * @param section 기사 섹션 (POLITICS, ECONOMY, SOCIAL, TECHNOLOGY)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (최대 100)
     */
    @GET("articles")
    suspend fun getArticles(
        @Query("section") section: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<ArticleResponse>>

    /**
     * 섹션별 랜덤 기사 조회
     * @param section 기사 섹션 (POLITICS, ECONOMY, SOCIAL, TECHNOLOGY)
     * @param date 조회할 날짜 (yyyy-MM-dd), 미지정 시 오늘 날짜
     */
    @GET("articles/{section}/random")
    suspend fun getRandomArticle(
        @Path("section") section: String,
        @Query("date") date: String? = null
    ): Response<ArticleRandomResponse>

    /**
     * 기사 반응 추가/변경/삭제
     * - LIKE: 좋아요
     * - DISLIKE: 싫어요
     * - NONE: 반응 해제
     * @param articleId 기사 ID
     * @param request 반응 타입
     */
    @POST("articles/{articleId}/reaction")
    suspend fun reactToArticle(
        @Path("articleId") articleId: Long,
        @Body request: ReactionRequest
    ): Response<Unit>
}
