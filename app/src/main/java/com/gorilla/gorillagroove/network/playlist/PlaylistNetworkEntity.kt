package com.gorilla.gorillagroove.network.playlist

import com.google.gson.annotations.SerializedName

data class PlaylistNetworkEntity(
    @SerializedName("content")
    val content: List<PlaylistItemNetworkEntity>
)