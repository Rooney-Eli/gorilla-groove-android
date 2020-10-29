package com.example.ggmobileredux.network.playlist

import com.example.ggmobileredux.network.track.TrackNetworkEntity


data class PlaylistItemNetworkEntity(
    var id: Int,
    var track: TrackNetworkEntity,
    var createdAt: String,
    var updatedAt: String
)