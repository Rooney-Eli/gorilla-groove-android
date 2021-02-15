package com.gorilla.gorillagroove.database

import androidx.room.*

@Dao
interface DatabaseDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(trackEntity: TrackCacheEntity): Long //returns insertion row number

    @Query("SELECT * from tracks")
    suspend fun getAllTracks(): List<TrackCacheEntity>

    @Query("SELECT * from tracks ORDER BY name ASC")
    suspend fun  getAllTracksSortedAz(): List<TrackCacheEntity>

    @Query("SELECT * from tracks ORDER BY addedToLibrary ASC")
    suspend fun  getAllTracksSortedDateAddedOldest(): List<TrackCacheEntity>

    @Query("SELECT * from tracks ORDER BY addedToLibrary DESC")
    suspend fun  getAllTracksSortedDateAddedNewest(): List<TrackCacheEntity>

    @Query("SELECT * FROM tracks WHERE trackId = :trackId")
    suspend fun getTrackById(trackId: Long) : TrackCacheEntity

    @Update
    suspend fun updateTrack(track: TrackCacheEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserCacheEntity): Long

    @Query("SELECT * from users")
    suspend fun getAllUsers(): List<UserCacheEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistKey(playlistKeyEntity: PlaylistKeyCacheEntity): Long

    @Query("SELECT * from playlists")
    suspend fun getAllPlaylists(): List<PlaylistKeyCacheEntity>

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()


    @Query("SELECT * from playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistKeyCacheEntity


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(trackEntity: TrackCacheEntity): Long

    @Query("SELECT * from tracks")
    suspend fun getAllPlaylistTracks(): List<TrackCacheEntity>



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistReferenceData(playlistItemEntity: PlaylistItemReferenceData): Long

    @Query("SELECT * from playlist_items WHERE playlistId = :playlistId")
    suspend fun getPlaylistReferenceData(playlistId: Long): List<PlaylistItemReferenceData>

    @Query("DELETE FROM playlist_items")
    suspend fun deleteAllPlaylistData()



}