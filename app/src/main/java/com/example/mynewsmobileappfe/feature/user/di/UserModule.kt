package com.example.mynewsmobileappfe.feature.user.di

import com.example.mynewsmobileappfe.feature.user.data.repository.UserRepositoryImpl
import com.example.mynewsmobileappfe.feature.user.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * User 도메인에서 사용되는 의존성을 Hilt로 제공하는 모듈입니다.
 *
 * UserRepository 인터페이스와
 * 해당 구현체인 UserRepositoryImpl을 바인딩하여,
 * 의존성 주입 시 구현체가 아닌 인터페이스에 의존하도록 합니다.
 *
 * @Binds를 사용해 UserRepositoryImpl을
 * UserRepository 타입으로 주입하며,
 * @Singleton 범위로 앱 전체에서 하나의 인스턴스를 공유합니다.
 *
 * SingletonComponent에 설치되어
 * 애플리케이션 생명주기 동안 유지됩니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

}