package com.example.mynewsmobileappfe.core.datastore.di

import android.content.Context
import com.example.mynewsmobileappfe.core.datastore.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/**
 * TokenManager를 Hilt로 전역 DI 제공
 */
@Module
@InstallIn(SingletonComponent::class)
// SingletonComponent → 앱 전체 생명주기 동안 유지
object DataStoreModule {

    // 이걸 통해 어디서든 @Inject TokenManager 를 바로 사용할 수 있음.
    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

}
