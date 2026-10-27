package com.example.stepcounter.di

import android.content.Context
import androidx.room.Room
import com.example.stepcounter.data.local.AlarmDao
import com.example.stepcounter.data.db.AlarmDatabase
import com.example.stepcounter.data.db.MIGRATION_4_5
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
    fun provideAlarmDatabase(
        @ApplicationContext context: Context
    ) : AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            "alarms"
        )
            .addMigrations(MIGRATION_4_5) // Before 4->5, fallbackToDestructiveMigration() was being used.
            .build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(alarmDatabase: AlarmDatabase) : AlarmDao {
        return alarmDatabase.alarmDao()
    }
}