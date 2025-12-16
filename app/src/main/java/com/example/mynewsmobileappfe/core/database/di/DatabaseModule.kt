package com.example.mynewsmobileappfe.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.mynewsmobileappfe.core.database.AppDatabase
import com.example.mynewsmobileappfe.core.database.dao.HighlightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room Database DI Module
 *
 * AppDatabase와 DAO를 Hilt로 제공합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // 스키마 변경 시 기존 데이터 삭제 (개발 중)
            .build()
    }

    @Provides
    @Singleton
    fun provideHighlightDao(database: AppDatabase): HighlightDao {
        return database.highlightDao()
    }
}