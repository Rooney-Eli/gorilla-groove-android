package com.example.ggmobileredux.database

import com.example.ggmobileredux.model.PlaylistKey
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.model.User
import javax.inject.Inject

class CacheMapper
@Inject
constructor() {

    //Track

    fun mapFromTrackEntity(entity: TrackCacheEntity): Track {
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

    fun mapToTrackEntity(domainModel: Track): TrackCacheEntity {
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

    fun mapFromTrackEntityList(entities: List<TrackCacheEntity>): List<Track> {
        return entities.map { mapFromTrackEntity(it) }
    }

    fun mapToTrackEntityList(tracks: List<Track>): List<TrackCacheEntity> {
        return tracks.map { mapToTrackEntity(it) }
    }

    //User

    fun mapFromUserEntity(entity: UserCacheEntity): User {
        return User(
            id = entity.id,
            username = entity.username,
            email = entity.email
        )
    }

    fun mapToUserEntity(domainModel: User): UserCacheEntity {
        return UserCacheEntity(
            id = domainModel.id,
            username = domainModel.username,
            email = domainModel.email

        )
    }

    fun mapFromUserEntityList(entities: List<UserCacheEntity>): List<User> {
        return entities.map { mapFromUserEntity(it) }
    }

    fun mapToUserEntityList(tracks: List<User>): List<UserCacheEntity> {
        return tracks.map { mapToUserEntity(it) }
    }

    //Playlist


    fun mapFromPlaylistEntity(entity: PlaylistKeyCacheEntity): PlaylistKey {
        return PlaylistKey(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt

        )
    }

    fun mapToPlaylistEntity(domainModel: PlaylistKey): PlaylistKeyCacheEntity {
        return PlaylistKeyCacheEntity(
            id = domainModel.id,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt
        )
    }

    fun mapFromPlaylistEntityList(entities: List<PlaylistKeyCacheEntity>): List<PlaylistKey> {
        return entities.map { mapFromPlaylistEntity(it) }
    }

    fun mapToPlaylistEntityList(tracks: List<PlaylistKey>): List<PlaylistKeyCacheEntity> {
        return tracks.map { mapToPlaylistEntity(it) }
    }


}