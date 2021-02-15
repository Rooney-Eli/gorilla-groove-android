package com.gorilla.gorillagroove.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_items")
data class PlaylistItemReferenceData(
    //Because I have been using backend generated table keys, this table is possible
    //a track was inserted in a table with key of it's id, aka the key of it's backend key

    //PK set to false so I can use GG backend id key
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "playlistItemId") //this is the id of the backend join table and will be unique
    var id: Long,

    @ColumnInfo(name = "playlistId") //this can be used to look up the playlist table for this info
    var playlistId: Long,

    @ColumnInfo(name = "trackId") // this can be used to look up the track table for this info
    var trackId: Long,

    @ColumnInfo(name = "createdAt")
    var createdAt: String,

    @ColumnInfo(name = "updatedAt")
    var updatedAt: String

)