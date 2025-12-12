package com.example.mynewsmobileappfe.core.network.di

import com.example.mynewsmobileappfe.core.datastore.TokenManager
import com.example.mynewsmobileappfe.core.network.interceptor.AuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://10.0.2.2:8080/api/v1/" // 에뮬레이터용, 실제론 변경 필요

    /**
     *
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    /**
     * Moshi 객체를 제공하는 프로바이더 메서드
     * - Moshi는 JSON 파싱을 위한 라이브러리 (JSON <-> Kotlin Object)
     * - KotlinJsonAdapterFactory
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * HttpLoggingInterceptor를 제공하는 Provider 함수.
     * - 네트워크 요청/응답의 로그를 확인하기 위해 사용됨
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    /**
     * 설정된 OkHttpClient 인스턴스를 제공하는 Provider 함수.
     *
     * - HTTP 요청/응답 로그 출력을 위한 LoggingInterceptor 추가
     * - 연결/읽기/쓰기 타임아웃 설정
     * - Retrofit 네트워크 통신에 사용되는 싱글톤 클라이언트
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // jwt 검증용
            .addInterceptor(loggingInterceptor) // logging 용
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit 인스턴스를 구성하여 제공하는 Provider 함수.
     *
     * - 모든 API 요청의 기본 baseUrl 설정
     * - Kotlin 친화적인 Moshi 직렬화/역직렬화 사용
     * - Interceptor 및 Timeout 설정이 적용된 OkHttpClient 연결
     *
     * Hilt를 통해 싱글톤으로 제공되며, 각종 API Service 생성에 사용된다.
     */
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
