package com.example.ggmobileredux.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistCacheEntity(
    //PK set to false so I can use GG backend id key
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "createdAt")
    var createdAt: String,

    @ColumnInfo(name = "updatedAt")
    var updatedAt: String

)