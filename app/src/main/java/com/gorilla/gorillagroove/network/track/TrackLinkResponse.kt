package com.gorilla.gorillagroove.network.track

import com.google.gson.annotations.SerializedName

data class TrackLinkResponse(

    @SerializedName("songLink")
    val trackLink: String,
    val albumArtLink: String?
)