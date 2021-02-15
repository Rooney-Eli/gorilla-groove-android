package com.gorilla.gorillagroove.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    TrackCacheEntity::class,
    UserCacheEntity::class,
    PlaylistKeyCacheEntity::class,
    PlaylistItemReferenceData::class
], version = 1)

abstract class GorillaDatabase: RoomDatabase() {

    abstract fun databaseDao(): DatabaseDao

    companion object {
        val DATABASE_NAME: String = "gorilla_db"
    }
}