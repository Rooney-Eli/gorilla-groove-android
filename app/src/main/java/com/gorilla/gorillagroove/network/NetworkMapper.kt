package com.gorilla.gorillagroove.network

import com.gorilla.gorillagroove.model.*
import com.gorilla.gorillagroove.network.login.LoginResponseNetworkEntity
import com.gorilla.gorillagroove.network.playlist.PlaylistKeyNetworkEntity
import com.gorilla.gorillagroove.network.playlist.PlaylistItemNetworkEntity
import com.gorilla.gorillagroove.network.playlist.PlaylistNetworkEntity
import com.gorilla.gorillagroove.network.track.TrackNetworkEntity
import javax.inject.Inject


class NetworkMapper
@Inject
constructor() {

    //Track

    fun mapFromTrackEntity(entity: TrackNetworkEntity): Track {
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

    fun mapToTrackEntity(domainModel: Track): TrackNetworkEntity {
        return TrackNetworkEntity(
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


    fun mapFromTrackEntityList(entities: List<TrackNetworkEntity>): List<Track> {
        return entities.map { mapFromTrackEntity(it) }
    }


    //Login

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


    //User

    fun mapFromUserEntity(entity: UserNetworkEntity): User {
        return User(
            id = entity.id,
            username = entity.username,
            email = entity.email
        )
    }

    fun mapToUserEntity(domainModel: User): UserNetworkEntity {
        return UserNetworkEntity(
            id = domainModel.id,
            username = domainModel.username,
            email = domainModel.email
        )
    }

    fun mapFromUserEntityList(entities: List<UserNetworkEntity>): List<User> {
        return entities.map { mapFromUserEntity(it) }
    }


    //Playlist Key

    fun mapFromPlaylistKeyEntity(entity: PlaylistKeyNetworkEntity): PlaylistKey {
        return PlaylistKey(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt

        )
    }

    fun mapToPlaylistKeyEntity(domainModel: PlaylistKey): PlaylistKeyNetworkEntity {
        return PlaylistKeyNetworkEntity(
            id = domainModel.id,
            name = domainModel.name,
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt
        )
    }

    fun mapFromPlaylistKeyEntityList(entities: List<PlaylistKeyNetworkEntity>): List<PlaylistKey> {
        return entities.map { mapFromPlaylistKeyEntity(it) }
    }



    //Playlist content

    fun mapFromPlaylistEntity(entity: PlaylistItemNetworkEntity): PlaylistItem {
        return PlaylistItem(
            id = entity.id,
            track = mapFromTrackEntity(entity.track),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt

        )
    }

    fun mapToPlaylistEntity(domainModel: PlaylistItem): PlaylistItemNetworkEntity {
        return PlaylistItemNetworkEntity(
            id = domainModel.id,
            track = mapToTrackEntity(domainModel.track),
            createdAt = domainModel.createdAt,
            updatedAt = domainModel.updatedAt
        )
    }

    fun mapFromPlaylistEntityList(entities: List<PlaylistItemNetworkEntity>): List<PlaylistItem> {
        return entities.map { mapFromPlaylistEntity(it) }
    }

    fun mapToPlaylist(
        playlistKey: PlaylistKey,
        playlistNetworkEntity: PlaylistNetworkEntity
    ): Playlist {
        return Playlist(
            id = playlistKey.id,
            name = playlistKey.name,
            playlistItems = mapFromPlaylistEntityList(playlistNetworkEntity.content),
            createdAt = playlistKey.createdAt,
            updatedAt = playlistKey.updatedAt

        )
    }


}