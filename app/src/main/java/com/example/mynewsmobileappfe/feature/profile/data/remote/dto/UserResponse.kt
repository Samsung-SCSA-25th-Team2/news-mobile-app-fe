package com.example.mynewsmobileappfe.feature.profile.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: Long,
    val email: String,
    val bookmarkCount: Int
)