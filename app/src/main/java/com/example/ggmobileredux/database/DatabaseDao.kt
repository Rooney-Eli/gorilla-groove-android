package com.example.ggmobileredux.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserCacheEntity): Long

    @Query("SELECT * from users")
    suspend fun getAllUsers(): List<UserCacheEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlistEntity: PlaylistCacheEntity): Long

    @Query("SELECT * from playlists")
    suspend fun getAllPlaylists(): List<PlaylistCacheEntity>






}