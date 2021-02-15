package com.gorilla.gorillagroove.network

import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.login.LoginResponseNetworkEntity
import com.gorilla.gorillagroove.network.playlist.PlaylistKeyNetworkEntity
import com.gorilla.gorillagroove.network.playlist.PlaylistNetworkEntity
import com.gorilla.gorillagroove.network.track.TrackLinkResponse
import com.gorilla.gorillagroove.network.track.TrackNetworkEntity
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.network.track.TrackWrapper
import retrofit2.http.*

interface NetworkApi {
    @GET("/api/track?page=0&size=4000&sort=id,asc")
    suspend fun get(@Header("Authorization") token: String): TrackWrapper

    @GET("api/track/{id}")
    suspend fun getTrack(@Header("Authorization") token: String, @Path("id") songId: Long ): TrackNetworkEntity

    @GET("/api/file/link/{id}?artSize=SMALL")
    suspend fun getTrackLink(@Header("Authorization") token: String, @Path("id") songId: Long): TrackLinkResponse

    @POST("/api/authentication/login")
    suspend fun getAuthorization(@Body loginRequest: LoginRequest): LoginResponseNetworkEntity

    @GET("api/user")
    suspend fun getAllUsers(@Header("Authorization") token: String ): List<UserNetworkEntity>

    @GET("api/playlist")
    suspend fun getAllPlaylists(@Header("Authorization") token: String): List<PlaylistKeyNetworkEntity>

    @GET("api/playlist/track")
    suspend fun getAllPlaylistTracks(
        @Header("Authorization") token: String,
        @Query("playlistId") playlistId: Long,
        @Query("sort") sort: String,
        @Query("size") size: Long
    ): PlaylistNetworkEntity


    @PUT("api/track/simple-update")
    suspend fun updateTrack(
        @Header("Authorization") token: String,
        @Body updateTrackJson: TrackUpdate
    )

}
