package com.example.ggmobileredux.model

data class Track(
    var id: Int,
    var name: String = "",
    var album: String = "",
    var artist: String = "",
    var trackLink: String?,
    var albumArtLink: String?,
    var length: Long,
    var addedToLibrary: String
)