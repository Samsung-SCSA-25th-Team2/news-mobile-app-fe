package com.example.mynewsmobileappfe.core.network.di

import com.example.mynewsmobileappfe.feature.article.data.remote.api.ArticleApiService
import com.example.mynewsmobileappfe.feature.auth.data.remote.api.AuthApiService
import com.example.mynewsmobileappfe.feature.bookmark.data.remote.api.BookmarkApiService
import com.example.mynewsmobileappfe.feature.user.data.remote.api.UserApiService
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 애플리케이션에서 네트워크 통신에 사용되는
 * 다양한 API 서비스 인터페이스 인스턴스를 제공하는 모듈입니다.
 *
 * 본 모듈은 Dagger Hilt를 사용한 의존성 주입(DI)을 통해
 * 필요한 API 서비스들을 Singleton으로 제공합니다.
 * 각 서비스는 `@ApiRetrofit`으로 구분된 Retrofit 인스턴스를 사용하여 생성됩니다.
 *
 * 제공되는 API 서비스는 다음과 같습니다:
 * - AuthApiService: 로그인, 토큰 재발급 등 인증 관련 네트워크 요청을 처리합니다.
 * - UserApiService: 사용자 정보 조회 등 사용자 관련 네트워크 요청을 담당합니다.
 * - ArticleApiService: 기사 조회, 반응(좋아요/싫어요) 등 기사 관련 기능을 처리합니다.
 * - BookmarkApiService: 북마크 추가 및 삭제 등 북마크 관련 기능을 담당합니다.
 *
 * `@ApiRetrofit` Qualifier는
 * 일반적인 API 통신을 위해 설정된 Retrofit 인스턴스를
 * 다른 Retrofit 설정과 구분하기 위해 사용됩니다.
 */
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideAuthApiService(
        @ApiRetrofit retrofit: Retrofit
    ): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideUserApiService(
        @ApiRetrofit retrofit: Retrofit
    ): UserApiService = retrofit.create(UserApiService::class.java)

    @Provides
    @Singleton
    fun provideArticleApiService(
        @ApiRetrofit retrofit: Retrofit
    ): ArticleApiService = retrofit.create(ArticleApiService::class.java)

    @Provides
    @Singleton
    fun provideBookmarkApiService(
        @ApiRetrofit retrofit: Retrofit
    ): BookmarkApiService = retrofit.create(BookmarkApiService::class.java)

}