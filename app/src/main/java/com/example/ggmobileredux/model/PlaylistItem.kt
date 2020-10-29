package com.example.ggmobileredux.model

data class PlaylistItem(
    var id: Int,
    var track: Track,
    var createdAt: String,
    var updatedAt: String
)