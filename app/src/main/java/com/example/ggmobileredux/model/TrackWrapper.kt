package com.example.ggmobileredux.model

import com.example.ggmobileredux.network.TrackNetworkEntity
import com.google.gson.annotations.SerializedName


data class TrackWrapper(
    @SerializedName("content")
    val trackList: List<TrackNetworkEntity>
)
