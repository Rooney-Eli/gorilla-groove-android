package com.gorilla.gorillagroove.network.track

import com.google.gson.annotations.SerializedName


data class TrackWrapper(
    @SerializedName("content")
    val trackList: List<TrackNetworkEntity>
)
