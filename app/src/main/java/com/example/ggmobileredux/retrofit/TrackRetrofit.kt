package com.example.ggmobileredux.retrofit

import com.example.ggmobileredux.model.TrackWrapper
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface TrackRetrofit {
    @GET("/api/track?page=0&size=4000&sort=id,asc")
    suspend fun get(@Header("Authorization") token: String): TrackWrapper

    @GET("api/track/{id}")
    suspend fun getTrack(@Header("Authorization") token: String, @Path("id") songId: Int ): TrackNetworkEntity

    @GET("/api/file/link/{id}?artSize=SMALL")
    suspend fun getTrackLink(@Header("Authorization") token: String, @Path("id") songId: Int): TrackLinkResponse

    @POST("/api/authentication/login")
    suspend fun getAuthorization(@Body loginRequest: LoginRequest): LoginResponseNetworkEntity
}
