package com.example.ggmobileredux.retrofit

import com.example.ggmobileredux.model.LoginResponse
import com.example.ggmobileredux.model.Track
import javax.inject.Inject


class NetworkMapper
@Inject
constructor() {
    fun mapFromTrackEntity(entity: TrackNetworkEntity): Track {
        return Track(
            id = entity.id,
            name = entity.name,
            album = entity.album,
            artist = entity.artist,
            trackLink = entity.trackLink,
            albumArtLink = entity.albumArtLink,
            length = entity.length,
            addedToLibrary = entity.addedToLibrary
        )
    }

    fun mapToTrackEntity(domainModel: Track): TrackNetworkEntity {
        return TrackNetworkEntity(
            id = domainModel.id,
            name = domainModel.name,
            album = domainModel.album,
            artist = domainModel.artist,
            trackLink = domainModel.trackLink,
            albumArtLink = domainModel.albumArtLink,
            length = domainModel.length,
            addedToLibrary = domainModel.addedToLibrary
        )
    }


    fun mapFromTrackEntityList(entities: List<TrackNetworkEntity>): List<Track> {
        return entities.map { mapFromTrackEntity(it) }
    }


    fun mapFromLoginEntity(entity: LoginResponseNetworkEntity): LoginResponse {
        return LoginResponse(
            id = entity.id,
            token = entity.token,
            email = entity.email,
            username = entity.username
        )
    }

    fun mapToLoginEntity(domainModel: LoginResponse): LoginResponseNetworkEntity {
        return LoginResponseNetworkEntity(
            id = domainModel.id,
            token = domainModel.token,
            email = domainModel.email,
            username = domainModel.username
        )
    }

}