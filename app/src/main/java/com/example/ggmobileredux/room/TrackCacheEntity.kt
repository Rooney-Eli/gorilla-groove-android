package com.example.ggmobileredux.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackCacheEntity(
    //PK set to false so I can use GG backend id key
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "album")
    var album: String,

    @ColumnInfo(name = "artist")
    var artist: String,

    @ColumnInfo(name = "trackLink")
    var trackLink: String?,

    @ColumnInfo(name = "albumArtLink")
    var albumArtLink: String?,

    @ColumnInfo(name = "length")
    var length: Long,

    @ColumnInfo(name = "addedToLibrary")
    var addedToLibrary: String


)