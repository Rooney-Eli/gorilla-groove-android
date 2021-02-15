package com.gorilla.gorillagroove.network.playlist

import com.gorilla.gorillagroove.network.track.TrackNetworkEntity


data class PlaylistItemNetworkEntity(
    var id: Long,
    var track: TrackNetworkEntity,
    var createdAt: String,
    var updatedAt: String
)