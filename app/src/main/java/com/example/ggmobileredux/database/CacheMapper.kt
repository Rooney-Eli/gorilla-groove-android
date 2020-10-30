package com.example.ggmobileredux.database

import com.example.ggmobileredux.model.*
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

    //Playlist Key
    fun mapFromPlaylistKeyEntity(entity: PlaylistKeyCacheEntity): PlaylistKey {
        return PlaylistKey(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt

        )
    }

    fun mapToPlaylistKeyEntity(domainModel: PlaylistKey): PlaylistKeyCacheEntity {
        return PlaylistKeyCacheEntity(
            id = domainModel.id,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt
        )
    }



    fun mapFromPlaylistEntityList(entities: List<PlaylistKeyCacheEntity>): List<PlaylistKey> {
        return entities.map { mapFromPlaylistKeyEntity(it) }
    }

    fun mapToPlaylistEntityList(tracks: List<PlaylistKey>): List<PlaylistKeyCacheEntity> {
        return tracks.map { mapToPlaylistKeyEntity(it) }
    }

    fun mapToPlaylistItemList(playlist: Playlist): List<PlaylistItemReferenceData> {
        val playlistId = playlist.id

        val list = playlist.playlistItems.map {
            PlaylistItemReferenceData(
                id = it.id, //table insertion position
                playlistId = playlistId, //associated playlist position
                trackId = it.track.id, //associated track position
                createdAt = it.createdAt, //when the playlist item was made
                updatedAt = it.updatedAt //when the playlist item was updated
            )
        }
        return list
    }



}