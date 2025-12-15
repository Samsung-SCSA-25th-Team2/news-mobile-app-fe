package com.example.mynewsmobileappfe.feature.news.domain.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleRandomResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.model.Section
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {

    /**
     * 섹션별 기사 목록 조회
     * @param section 기사 섹션
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     */
    fun getArticles(
        section: Section,
        page: Int = 0,
        size: Int = 20
    ): Flow<Resource<PageResponse<ArticleResponse>>>

    /**
     * 섹션별 랜덤 기사 조회
     * @param section 기사 섹션
     * @param date 조회할 날짜 (yyyy-MM-dd), null이면 오늘 날짜
     */
    fun getRandomArticle(
        section: Section,
        date: String? = null
    ): Flow<Resource<ArticleRandomResponse>>

    /**
     * 기사 반응 추가/변경/삭제
     * @param articleId 기사 ID
     * @param reactionType 반응 타입 (LIKE, DISLIKE, NONE)
     */
    fun reactToArticle(
        articleId: Long,
        reactionType: ReactionType
    ): Flow<Resource<Unit>>
}