package com.example.mynewsmobileappfe.core.network.di

import com.example.mynewsmobileappfe.core.network.di.NetworkConfig.BASE_URL
import com.example.mynewsmobileappfe.feature.auth.data.remote.api.AuthApiService
import com.example.mynewsmobileappfe.feature.auth.data.remote.api.TokenRefreshApiService
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 토큰 재발급 전용 네트워크 구성.
 *
 * - 인터셉터/Authenticator 없이 순수한 클라이언트로 순환 의존성을 방지한다.
 * - @Qualifier를 이용하여 구분하고 구현!
 */
@Module
@InstallIn(SingletonComponent::class)
object TokenRefreshNetworkModule {

    // auth용
    @Provides
    @Singleton
    @TokenRefreshOkHttp
    fun provideAuthOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @TokenRefreshRetrofit
    fun provideAuthRetrofit(
        @TokenRefreshOkHttp okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    @TokenRefreshApi
    fun provideTokenRefreshApiService(
        @TokenRefreshRetrofit retrofit: Retrofit
    ): TokenRefreshApiService = retrofit.create(TokenRefreshApiService::class.java)

}
