package com.example.ggmobileredux.model

data class Track(
    var id: Long,
    var name: String = "",
    var artist: String = "",
    var featuring: String = "",
    var album: String = "",
    var trackNumber: Long?,
    var filename: String? = "",
    var bitRate: Long,
    var sampleRate: Long,
    var length: Long,
    var releaseYear: String? = "",
    var genre: String? = "",
    var playCount: Long,
    var pri: Boolean = false,
    var hidden: Boolean = false,
    var lastPlayed: String? = "",
    var createdAt: String = "",
    var addedToLibrary: String? = "",
    var note: String? = "",
    var inReview: Boolean = false,
    var hasArt: Boolean = false,
    var songUpdatedAt: String = "",
    var artUpdatedAt: String = "",

    var trackLink: String?,
    var albumArtLink: String?
)