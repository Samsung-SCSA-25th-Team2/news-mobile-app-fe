package com.example.mynewsmobileappfe.core.datastore.di

import android.content.Context
import com.example.mynewsmobileappfe.core.datastore.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * TokenManager를 Hilt로 전역 DI 제공
 */
@Module
// Hilt DI용 설계도 명시
@InstallIn(SingletonComponent::class)
// SingletonComponent → 앱 전체 생명주기 동안 유지
object DataStoreModule {

    @Provides // TokenManager가 필요하면, 이 함수를 실행해서 만들어라 -> Hilt가 사용. 나중에 @Inject로 주입하기만 하면됨
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context // 앱 전역에서 Context를 유지해야함
    ): TokenManager {
        return TokenManager(context) // TokenManager 생성자를 직접 호출해서 인스턴스 생성
    }

}
