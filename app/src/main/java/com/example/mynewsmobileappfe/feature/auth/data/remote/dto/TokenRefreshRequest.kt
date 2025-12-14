package com.example.mynewsmobileappfe.feature.auth.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenRefreshRequest(
    val accessToken: String,
    val refreshToken: String
)
