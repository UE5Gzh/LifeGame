package com.example.lifegame.di

import android.content.Context
import androidx.room.Room
import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifegame_database"
        ).build()
    }

    @Provides
    fun provideAttributeDao(appDatabase: AppDatabase): AttributeDao {
        return appDatabase.attributeDao()
    }
}
