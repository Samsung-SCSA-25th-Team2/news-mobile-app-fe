package com.example.mynewsmobileappfe.feature.news.data.repository

import com.example.mynewsmobileappfe.core.database.dao.HighlightDao
import com.example.mynewsmobileappfe.core.database.entity.Highlight
import com.example.mynewsmobileappfe.feature.news.domain.repository.HighlightRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 형광펜 하이라이트 Repository 구현
 *
 * Room Database를 사용하여 로컬에 하이라이트 정보를 저장합니다.
 */
class HighlightRepositoryImpl @Inject constructor(
    private val highlightDao: HighlightDao
) : HighlightRepository {

    override fun getHighlightsByArticleId(articleId: Long): Flow<List<Highlight>> {
        return highlightDao.getHighlightsByArticleId(articleId)
    }

    override suspend fun getHighlightsByArticleIdOnce(articleId: Long): List<Highlight> {
        return highlightDao.getHighlightsByArticleIdOnce(articleId)
    }

    override suspend fun addHighlight(
        articleId: Long,
        startIndex: Int,
        endIndex: Int,
        text: String,
        color: String
    ): Long {
        val highlight = Highlight(
            articleId = articleId,
            startIndex = startIndex,
            endIndex = endIndex,
            text = text,
            color = color
        )
        return highlightDao.insertHighlight(highlight)
    }

    override suspend fun deleteHighlight(highlightId: Long) {
        highlightDao.deleteHighlightById(highlightId)
    }

    override suspend fun deleteAllHighlightsByArticleId(articleId: Long) {
        highlightDao.deleteAllHighlightsByArticleId(articleId)
    }

    override suspend fun updateHighlightColor(highlightId: Long, newColor: String) {
        highlightDao.updateHighlightColor(highlightId, newColor)
    }
}