package com.example.mynewsmobileappfe.feature.news.ui

import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse

/**
 * 기사 상세 화면 상태
 */
sealed class ArticleDetailState {
    object Idle : ArticleDetailState()
    object Loading : ArticleDetailState()
    data class Success(val article: ArticleResponse) : ArticleDetailState()
    data class Error(val message: String) : ArticleDetailState()
}
