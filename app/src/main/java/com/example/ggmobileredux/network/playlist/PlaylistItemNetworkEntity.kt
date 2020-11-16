package com.example.ggmobileredux.network.playlist

import com.example.ggmobileredux.network.track.TrackNetworkEntity


data class PlaylistItemNetworkEntity(
    var id: Long,
    var track: TrackNetworkEntity,
    var createdAt: String,
    var updatedAt: String
)