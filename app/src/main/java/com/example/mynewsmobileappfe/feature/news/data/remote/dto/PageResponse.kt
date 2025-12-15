package com.example.mynewsmobileappfe.feature.news.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * 페이지네이션 응답
 */
@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)
