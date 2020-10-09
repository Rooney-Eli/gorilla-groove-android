package com.example.ggmobileredux.room

import com.example.ggmobileredux.model.Track
import javax.inject.Inject

class CacheMapper
@Inject
constructor() {
    fun mapFromEntity(entity: TrackCacheEntity): Track {
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

    fun mapToEntity(domainModel: Track): TrackCacheEntity {
        return TrackCacheEntity(
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

    fun mapFromEntityList(entities: List<TrackCacheEntity>): List<Track> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(tracks: List<Track>): List<TrackCacheEntity> {
        return tracks.map { mapToEntity(it) }
    }


}