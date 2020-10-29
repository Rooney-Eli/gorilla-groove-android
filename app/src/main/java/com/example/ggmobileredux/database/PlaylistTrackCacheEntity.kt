package com.example.ggmobileredux.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackCacheEntity (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "trackId")
    var id: Int,

    @ColumnInfo(name = "track")
    var track: TrackCacheEntity,

    @ColumnInfo(name = "createdAt")
    var createdAt: String,

    @ColumnInfo(name = "updatedAt")
    var updatedAt: String

)