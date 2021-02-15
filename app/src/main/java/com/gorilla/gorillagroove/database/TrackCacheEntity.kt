package com.gorilla.gorillagroove.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackCacheEntity(
    //PK set to false so I can use GG backend id key
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "trackId")
    var id: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "featuring")
    var featuring: String = "",

    @ColumnInfo(name = "album")
    var album: String,

    @ColumnInfo(name = "artist")
    var artist: String,

    @ColumnInfo(name = "length")
    var length: Long,

    @ColumnInfo(name = "addedToLibrary")
    var addedToLibrary: String?,

    @ColumnInfo(name = "trackNumber")
    var trackNumber: Int?,

    @ColumnInfo(name = "filename")
    var filename: String? = "",

    @ColumnInfo(name = "bitRate")
    var bitRate: Long,

    @ColumnInfo(name = "sampleRate")
    var sampleRate: Long,

    @ColumnInfo(name = "releaseYear")
    var releaseYear: Int?,

    @ColumnInfo(name = "genre")
    var genre: String? = "",

    @ColumnInfo(name = "playCount")
    var playCount: Long,

    @ColumnInfo(name = "pri")
    var pri: Boolean = false,

    @ColumnInfo(name = "hidden")
    var hidden: Boolean = false,

    @ColumnInfo(name = "lastPlayed")
    var lastPlayed: String? = "",

    @ColumnInfo(name = "createdAt")
    var createdAt: String = "",

    @ColumnInfo(name = "note")
    var note: String? = "",

    @ColumnInfo(name = "inReview")
    var inReview: Boolean = false,

    @ColumnInfo(name = "hasArt")
    var hasArt: Boolean = false,

    @ColumnInfo(name = "songUpdatedAt")
    var songUpdatedAt: String = "",

    @ColumnInfo(name = "artUpdatedAt")
    var artUpdatedAt: String = "",

    @ColumnInfo(name = "trackLink")
    var trackLink: String?,

    @ColumnInfo(name = "albumArtLink")
    var albumArtLink: String?

)