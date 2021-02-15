package com.gorilla.gorillagroove.database

import androidx.room.*

@Entity(tableName = "playlists")
data class PlaylistKeyCacheEntity(
    //PK set to false so I can use GG backend id key
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "playlistId")
    var id: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "createdAt")
    var createdAt: String,

    @ColumnInfo(name = "updatedAt")
    var updatedAt: String

)
