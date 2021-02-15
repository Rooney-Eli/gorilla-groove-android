package com.gorilla.gorillagroove.di

import android.content.Context
import androidx.room.Room
import com.gorilla.gorillagroove.database.DatabaseDao
import com.gorilla.gorillagroove.database.GorillaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideTrackDb(@ApplicationContext context: Context): GorillaDatabase {
        return Room.databaseBuilder(
            context,
            GorillaDatabase::class.java,
            GorillaDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    }

    @Singleton
    @Provides
    fun provideTrackDAO(gorillaDatabase: GorillaDatabase): DatabaseDao {
        return gorillaDatabase.databaseDao()
    }

}