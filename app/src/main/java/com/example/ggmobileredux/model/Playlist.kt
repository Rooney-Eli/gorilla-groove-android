package com.example.ggmobileredux.model

data class Playlist(
    var id : Int,
    var name: String,
    var playlistItems: List<PlaylistItem>,
    var createdAt: String,
    var updatedAt: String
)