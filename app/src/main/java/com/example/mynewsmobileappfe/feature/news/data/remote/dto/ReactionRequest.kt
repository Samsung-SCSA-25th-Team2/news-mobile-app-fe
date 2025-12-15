package com.example.mynewsmobileappfe.feature.news.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReactionRequest(
    val type: String  // LIKE, DISLIKE, NONE
)
