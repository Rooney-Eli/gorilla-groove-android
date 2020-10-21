package com.example.ggmobileredux.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trackEntity: TrackCacheEntity): Long //returns insertion row number

    @Query("SELECT * from tracks")
    suspend fun getAllTracks(): List<TrackCacheEntity>

    @Query("SELECT * from tracks ORDER BY name ASC")
    suspend fun  getAllTracksSortedAz(): List<TrackCacheEntity>

    @Query("SELECT * from tracks ORDER BY addedToLibrary ASC")
    suspend fun  getAllTracksSortedDateAddedOldest(): List<TrackCacheEntity>

    @Query("SELECT * from tracks ORDER BY addedToLibrary DESC")
    suspend fun  getAllTracksSortedDateAddedNewest(): List<TrackCacheEntity>




}