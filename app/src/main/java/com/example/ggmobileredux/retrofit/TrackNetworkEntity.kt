package com.example.ggmobileredux.retrofit

import com.google.gson.annotations.SerializedName

data class TrackNetworkEntity(

    @SerializedName("id")
    var id: Int,
    var name: String,
    var album: String,
    var artist: String,
    var trackLink: String?,
    var albumArtLink: String?,
    var length: Long,
    var addedToLibrary: String
)