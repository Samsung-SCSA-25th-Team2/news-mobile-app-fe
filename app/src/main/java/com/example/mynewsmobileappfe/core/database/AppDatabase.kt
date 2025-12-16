package com.example.mynewsmobileappfe.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mynewsmobileappfe.core.database.dao.HighlightDao
import com.example.mynewsmobileappfe.core.database.entity.Highlight

/**
 * Room Database
 *
 * 앱의 로컬 데이터베이스 (형광펜 하이라이트 등)
 */
@Database(
    entities = [Highlight::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun highlightDao(): HighlightDao

    companion object {
        const val DATABASE_NAME = "mynews_database"
    }
}