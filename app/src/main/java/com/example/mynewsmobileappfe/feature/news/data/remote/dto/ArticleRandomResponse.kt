package com.example.mynewsmobileappfe.feature.news.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * 랜덤 기사 조회 응답 (간소화 버전)
 */
@JsonClass(generateAdapter = true)
data class ArticleRandomResponse(
    val articleId: Long,
    val section: String,
    val title: String,
    val content: String? = null,
    val url: String,
    val thumbnailUrl: String?,
    val source: String,
    val publisher: String,
    val publishedAt: String,
    val bookmarked: Boolean = false,
    val userReaction: String? = null  // "LIKE", "DISLIKE", null
)
