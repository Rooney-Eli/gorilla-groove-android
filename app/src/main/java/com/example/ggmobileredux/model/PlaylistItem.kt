package com.example.ggmobileredux.model

data class PlaylistItem(
    var id: Long,
    var track: Track,
    var createdAt: String,
    var updatedAt: String
)