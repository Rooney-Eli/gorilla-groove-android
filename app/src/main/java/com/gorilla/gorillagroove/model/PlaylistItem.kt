package com.gorilla.gorillagroove.model

data class PlaylistItem(
    var id: Long,
    var track: Track,
    var createdAt: String,
    var updatedAt: String
)