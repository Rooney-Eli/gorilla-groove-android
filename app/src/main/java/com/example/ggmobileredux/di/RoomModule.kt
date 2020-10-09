package com.example.ggmobileredux.di

import android.content.Context
import androidx.room.Room
import com.example.ggmobileredux.room.TrackDao
import com.example.ggmobileredux.room.TrackDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object RoomModule {

    @Singleton
    @Provides
    fun provideTrackDb(@ApplicationContext context: Context): TrackDatabase {
        return Room.databaseBuilder(
            context,
            TrackDatabase::class.java,
            TrackDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    }

    @Singleton
    @Provides
    fun provideTrackDAO(trackDatabase: TrackDatabase): TrackDao {
        return trackDatabase.trackDao()
    }

}