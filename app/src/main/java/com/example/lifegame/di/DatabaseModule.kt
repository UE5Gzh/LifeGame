package com.example.lifegame.di

import android.content.Context
import androidx.room.Room
import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.dao.BehaviorDao
import com.example.lifegame.data.dao.LogDao
import com.example.lifegame.data.dao.QuestDao
import com.example.lifegame.data.dao.RankDao
import com.example.lifegame.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.example.lifegame.data.dao.BehaviorGroupDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `details` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifegame_database"
        )
        .addMigrations(MIGRATION_6_7)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideAttributeDao(appDatabase: AppDatabase): AttributeDao {
        return appDatabase.attributeDao()
    }

    @Provides
    fun provideRankDao(appDatabase: AppDatabase): RankDao {
        return appDatabase.rankDao()
    }

    @Provides
    fun provideBehaviorDao(appDatabase: AppDatabase): BehaviorDao {
        return appDatabase.behaviorDao()
    }

    @Provides
    fun provideBehaviorGroupDao(appDatabase: AppDatabase): BehaviorGroupDao {
        return appDatabase.behaviorGroupDao()
    }

    @Provides
    fun provideQuestDao(database: AppDatabase): QuestDao {
        return database.questDao()
    }

    @Provides
    fun provideLogDao(database: AppDatabase): LogDao {
        return database.logDao()
    }
}
