package com.example.mynewsmobileappfe.feature.bookmark.data.repository

import com.example.mynewsmobileappfe.core.common.Resource
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.ArticleResponse
import com.example.mynewsmobileappfe.feature.news.data.remote.dto.PageResponse
import com.example.mynewsmobileappfe.feature.bookmark.data.remote.api.BookmarkApiService
import com.example.mynewsmobileappfe.feature.bookmark.domain.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkApiService: BookmarkApiService
) : BookmarkRepository {

    override fun addBookmark(articleId: Long): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = bookmarkApiService.addBookmark(articleId)
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "로그인이 필요합니다."
                    404 -> "기사를 찾을 수 없습니다."
                    else -> "북마크 추가에 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun removeBookmark(articleId: Long): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = bookmarkApiService.removeBookmark(articleId)
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "로그인이 필요합니다."
                    else -> "북마크 삭제에 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)

    override fun getMyBookmarks(page: Int, size: Int): Flow<Resource<PageResponse<ArticleResponse>>> = flow {
        emit(Resource.Loading())
        try {
            val response = bookmarkApiService.getMyBookmarks(page, size)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "잘못된 요청입니다."
                    401 -> "로그인이 필요합니다."
                    else -> "북마크 목록을 불러오는데 실패했습니다."
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "네트워크 오류가 발생했습니다."))
        }
    }.flowOn(Dispatchers.IO)
}
