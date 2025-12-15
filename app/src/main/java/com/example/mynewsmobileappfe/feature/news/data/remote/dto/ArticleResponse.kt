package com.example.mynewsmobileappfe.feature.news.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * 기사 목록 조회 응답
 */
@JsonClass(generateAdapter = true)
data class ArticleResponse(
    val articleId: Long,
    val section: String,
    val title: String,
    val content: String? = null,  // 목록 조회 시에만 포함
    val url: String,
    val thumbnailUrl: String?,
    val source: String,
    val publisher: String,
    val publishedAt: String,      // ISO 8601 형식
    val likes: Int = 0,
    val dislikes: Int = 0,
    val bookmarked: Boolean = false,
    val userReaction: String? = null  // "LIKE", "DISLIKE", null (사용자의 반응 상태)
)
