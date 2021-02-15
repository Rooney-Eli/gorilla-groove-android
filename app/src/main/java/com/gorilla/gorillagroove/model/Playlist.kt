package com.gorilla.gorillagroove.model

data class Playlist(
    var id : Long,
    var name: String,
    var playlistItems: List<PlaylistItem>,
    var createdAt: String,
    var updatedAt: String
)