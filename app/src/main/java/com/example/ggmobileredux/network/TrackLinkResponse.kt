package com.example.ggmobileredux.network

import com.google.gson.annotations.SerializedName

data class TrackLinkResponse(

    @SerializedName("songLink")
    val trackLink: String,
    val albumArtLink: String?
)