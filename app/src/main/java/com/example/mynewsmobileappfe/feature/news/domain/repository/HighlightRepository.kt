package com.example.mynewsmobileappfe.feature.news.domain.repository

import com.example.mynewsmobileappfe.core.database.entity.Highlight
import kotlinx.coroutines.flow.Flow

/**
 * 형광펜 하이라이트 Repository 인터페이스
 *
 * 기사 본문에 대한 사용자의 형광펜 하이라이트를 관리합니다.
 */
interface HighlightRepository {

    /**
     * 특정 기사의 모든 하이라이트 조회 (실시간 업데이트)
     */
    fun getHighlightsByArticleId(articleId: Long): Flow<List<Highlight>>

    /**
     * 특정 기사의 모든 하이라이트 조회 (일회성)
     */
    suspend fun getHighlightsByArticleIdOnce(articleId: Long): List<Highlight>

    /**
     * 하이라이트 추가
     *
     * @return 생성된 하이라이트 ID
     */
    suspend fun addHighlight(
        articleId: Long,
        startIndex: Int,
        endIndex: Int,
        text: String,
        color: String
    ): Long

    /**
     * 하이라이트 삭제
     */
    suspend fun deleteHighlight(highlightId: Long)

    /**
     * 특정 기사의 모든 하이라이트 삭제
     */
    suspend fun deleteAllHighlightsByArticleId(articleId: Long)

    /**
     * 하이라이트 색상 변경
     */
    suspend fun updateHighlightColor(highlightId: Long, newColor: String)
}
