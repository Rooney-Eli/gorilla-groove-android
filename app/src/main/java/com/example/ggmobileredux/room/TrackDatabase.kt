package com.example.ggmobileredux.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TrackCacheEntity::class], version = 1)
abstract class TrackDatabase: RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        val DATABASE_NAME: String = "track_db"
    }
}