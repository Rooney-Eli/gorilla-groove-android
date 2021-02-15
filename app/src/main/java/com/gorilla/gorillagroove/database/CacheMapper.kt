package com.gorilla.gorillagroove.database

import com.gorilla.gorillagroove.model.*
import javax.inject.Inject

class CacheMapper
@Inject
constructor() {

    //Track
    fun mapFromTrackEntity(entity: TrackCacheEntity): Track {
        return Track(
            id = entity.id,
            name = entity.name,
            artist = entity.artist,
            featuring = entity.featuring,
            album = entity.album,
            trackNumber = entity.trackNumber,
            filename = entity.filename,
            bitRate = entity.bitRate,
            sampleRate = entity.sampleRate,
            length = entity.length,
            releaseYear = entity.releaseYear,
            genre = entity.genre,
            playCount = entity.playCount,
            pri = entity.pri,
            hidden = entity.hidden,
            lastPlayed = entity.lastPlayed,
            createdAt = entity.createdAt,
            addedToLibrary = entity.addedToLibrary,
            note = entity.note,
            inReview = entity.inReview,
            hasArt = entity.hasArt,
            songUpdatedAt = entity.songUpdatedAt,
            artUpdatedAt = entity.artUpdatedAt,
            trackLink = entity.trackLink,
            albumArtLink = entity.albumArtLink
        )
    }

    fun mapToTrackEntity(domainModel: Track): TrackCacheEntity {
        return TrackCacheEntity(
            id = domainModel.id,
            name = domainModel.name,
            artist = domainModel.artist,
            featuring = domainModel.featuring,
            album = domainModel.album,
            trackNumber = domainModel.trackNumber,
            filename = domainModel.filename,
            bitRate = domainModel.bitRate,
            sampleRate = domainModel.sampleRate,
            length = domainModel.length,
            releaseYear = domainModel.releaseYear,
            genre = domainModel.genre,
            playCount = domainModel.playCount,
            pri = domainModel.pri,
            hidden = domainModel.hidden,
            lastPlayed = domainModel.lastPlayed,
            createdAt = domainModel.createdAt,
            addedToLibrary = domainModel.addedToLibrary,
            note = domainModel.note,
            inReview = domainModel.inReview,
            hasArt = domainModel.hasArt,
            songUpdatedAt = domainModel.songUpdatedAt,
            artUpdatedAt = domainModel.artUpdatedAt,
            trackLink = domainModel.trackLink,
            albumArtLink = domainModel.albumArtLink
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