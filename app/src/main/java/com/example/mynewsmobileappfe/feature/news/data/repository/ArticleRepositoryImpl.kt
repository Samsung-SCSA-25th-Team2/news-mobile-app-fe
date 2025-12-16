package com.example.mynewsmobileappfe.feature.news.data.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.api.ArticleApiService
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleRandomResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ReactionRequest
import com.example.mynewsmobileappfe.feature.news.domain.model.ReactionType
import com.example.mynewsmobileappfe.feature.news.domain.model.Section
import com.example.mynewsmobileappfe.feature.news.domain.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val articleApiService: ArticleApiService
) : ArticleRepository {

    override fun getArticles(
        section: Section,
        page: Int,
        size: Int
    ): Flow<Resource<PageResponse<ArticleResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = articleApiService.getArticles(
                section = section.name,
                page = page,
                size = size
            )
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "잘못된 요청입니다."
                    else -> "기사를 불러오는데 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRandomArticle(
        section: Section,
        date: String?
    ): Flow<Resource<ArticleRandomResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = articleApiService.getRandomArticle(
                section = section.name,
                date = date
            )
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "잘못된 요청입니다."
                    404 -> "해당 조건의 기사를 찾을 수 없습니다."
                    else -> "기사를 불러오는데 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun reactToArticle(
        articleId: Long,
        reactionType: ReactionType
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = articleApiService.reactToArticle(
                articleId = articleId,
                request = ReactionRequest(type = reactionType.name)
            )
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "잘못된 반응 타입입니다."
                    401 -> "로그인이 필요합니다."
                    404 -> "기사를 찾을 수 없습니다."
                    else -> "반응 처리에 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)
}
