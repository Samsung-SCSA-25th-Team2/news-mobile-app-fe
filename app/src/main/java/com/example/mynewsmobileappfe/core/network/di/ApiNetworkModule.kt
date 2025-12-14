package com.example.mynewsmobileappfe.core.network.di

import com.example.mynewsmobileappfe.core.datastore.TokenManager
import com.example.mynewsmobileappfe.core.network.di.NetworkConfig.BASE_URL
import com.example.mynewsmobileappfe.core.network.interceptor.AuthInterceptor
import com.example.mynewsmobileappfe.core.network.interceptor.TokenAuthenticator
import com.example.mynewsmobileappfe.feature.auth.data.remote.api.AuthApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 일반 API 통신용 네트워크 모듈 (인터셉터/Authenticator 포함)
 *
 * - Hilt가 알아서 아래 @Provide @Singleton을 보고
 * - 구현한 메서드를 사용하여, @Inject 한 곳에 DI 해줌
 *
 * - !!! provideMoshi 등 메서드를 개발자가 사용하는 것이 절대로 아님 !!!
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiNetworkModule {

    // JSON ↔ Kotlin data class 변환기
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // TokenManager에서 accessToken을 읽어서
    // 매 요청마다 Authorization: Bearer ... 같은 헤더를 붙이는 역할
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager // Hilt에 주입된 것을 사용 -> 옆 도움 아이콘 확인
    ): AuthInterceptor = AuthInterceptor(tokenManager)

    // 401 응답이 왔을 때만 동작하는 OkHttp Authenticator
    // accessToken reissue 하기 위함
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        authApiService: AuthApiService
    ): TokenAuthenticator = TokenAuthenticator(tokenManager, authApiService)

    // logging용
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        // apply(스코프 함수) : HttpLoggingInterceptor의 level 속성을 BODY로 바꿈

    // OkHttpClient 조립 해서 Hilt에 DI
    // -> http에 대한 처리 로직 여기다가 체이닝 메서드로 구현
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // 화이트리스트 제외 모든 요청에 jwt header 붙이기
            .authenticator(tokenAuthenticator) // 401 응답 시, /reissue 요청
            .addInterceptor(loggingInterceptor) // logging
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    // 최종 rest api 담당하는 retrofit Hilt로 DI
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

}
