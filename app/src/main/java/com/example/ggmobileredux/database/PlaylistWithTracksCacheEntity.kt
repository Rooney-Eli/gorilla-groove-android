package com.example.ggmobileredux.database

import androidx.room.*

@Entity(tableName = "playlist_with_track")
data class PlaylistWithTracksCacheEntity(
    //PK set to false so I can use GG backend id key
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "playlistId")
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "createdAt")
    var createdAt: String,

    @ColumnInfo(name = "updatedAt")
    var updatedAt: String

)

@Entity(primaryKeys = ["playlistId", "songId"])
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val songId: Long
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistWithTracksCacheEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "trackId",
        associateBy = Junction(PlaylistTrackCrossRef::class)
    )
    val tracks: List<PlaylistTrackCacheEntity>
)