package com.example.mynewsmobileappfe.core.database.dao

import androidx.room.*
import com.example.mynewsmobileappfe.core.database.entity.Highlight
import kotlinx.coroutines.flow.Flow

/**
 * 형광펜 하이라이트 DAO
 *
 * 기사별 하이라이트 정보를 로컬 데이터베이스에서 관리합니다.
 */
@Dao
interface HighlightDao {

    /**
     * 특정 기사의 모든 하이라이트 조회 (Flow - 실시간 업데이트)
     */
    @Query("SELECT * FROM highlights WHERE articleId = :articleId ORDER BY startIndex ASC")
    fun getHighlightsByArticleId(articleId: Long): Flow<List<Highlight>>

    /**
     * 특정 기사의 모든 하이라이트 조회 (일회성)
     */
    @Query("SELECT * FROM highlights WHERE articleId = :articleId ORDER BY startIndex ASC")
    suspend fun getHighlightsByArticleIdOnce(articleId: Long): List<Highlight>

    /**
     * 하이라이트 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long

    /**
     * 하이라이트 삭제
     */
    @Delete
    suspend fun deleteHighlight(highlight: Highlight)

    /**
     * 하이라이트 ID로 삭제
     */
    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlightById(highlightId: Long)

    /**
     * 특정 기사의 모든 하이라이트 삭제
     */
    @Query("DELETE FROM highlights WHERE articleId = :articleId")
    suspend fun deleteAllHighlightsByArticleId(articleId: Long)

    /**
     * 하이라이트 색상 변경
     */
    @Query("UPDATE highlights SET color = :newColor WHERE id = :highlightId")
    suspend fun updateHighlightColor(highlightId: Long, newColor: String)
}