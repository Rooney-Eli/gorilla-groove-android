package com.example.ggmobileredux.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    TrackCacheEntity::class,
    UserCacheEntity::class,
    PlaylistCacheEntity::class], version = 1)

abstract class GorillaDatabase: RoomDatabase() {

    abstract fun databaseDao(): DatabaseDao

    companion object {
        val DATABASE_NAME: String = "gorilla_db"
    }
}