package com.example.ggmobileredux.repository

import android.content.SharedPreferences
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.ggmobileredux.network.*
import com.example.ggmobileredux.database.CacheMapper
import com.example.ggmobileredux.database.DatabaseDao
import com.example.ggmobileredux.model.*
import com.example.ggmobileredux.network.login.LoginRequest
import com.example.ggmobileredux.network.track.TrackLinkResponse
import com.example.ggmobileredux.util.Constants.KEY_SORT
import com.example.ggmobileredux.util.Constants.KEY_USER_TOKEN
import com.example.ggmobileredux.util.Constants.SORT_BY_AZ
import com.example.ggmobileredux.util.Constants.SORT_BY_DATE_ADDED_NEWEST
import com.example.ggmobileredux.util.Constants.SORT_BY_DATE_ADDED_OLDEST
import com.example.ggmobileredux.util.Constants.SORT_BY_ID
import com.example.ggmobileredux.util.DataState
import com.example.ggmobileredux.util.SessionState
import com.example.ggmobileredux.util.StateEvent
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import org.json.JSONObject


class MainRepository (
    private val databaseDao: DatabaseDao,
    private val networkApi: NetworkApi,
    private val cacheMapper: CacheMapper,
    private val networkMapper: NetworkMapper,
    private val sharedPreferences: SharedPreferences,
    private val dataSourceFactory: DefaultDataSourceFactory,
    private val okClient: OkHttpClient
) {
    private val TAG = "AppDebug: Repository"

    lateinit var webSocket: WebSocket

    private var userToken: String = sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""

    private var trackSorting: Sort = sharedPreferences.getString(KEY_SORT, SORT_BY_ID)?.toSort() ?: Sort.ID

    //glorified memory for lookups
    private val allTracks = LinkedHashMap<Int, Track>()
    private val allUsers = mutableListOf<User>()
    private val playlistKeys = mutableListOf<PlaylistKey>()
    private val playlists = mutableListOf<Playlist>()


    init {
        initWebSocket()
    }

    //KEEP THESE IN SYNC WITH EACH OTHER

    //everytime a new fragments tracks are fetched put em' here too
    val pendingConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val pendingMetadataList = mutableListOf<MediaMetadataCompat>()

    //if a user taps a track set this to pending tracks
    val playingConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val playingMetadataList = mutableListOf<MediaMetadataCompat>()


    //return pending tracks when a fragment's get tracks is activated
    val pendingTracks = mutableListOf<Track>()
    val playingTracks = mutableListOf<Track>()

    private var lastVerifiedTrack: Int? = null
    private lateinit var lastFetchedLinks: TrackLinkResponse


    fun stagePendingTracks() {
        if(playingTracks != pendingTracks) {
            playingTracks.clear()
            playingTracks.addAll(pendingTracks)
            playingConcatenatingMediaSource.clear()
            playingMetadataList.clear()
            pendingTracks.map {
                playingConcatenatingMediaSource.addCustomMediaSource(it)
                playingMetadataList.add(it.toMediaMetadataItem())
            }
        }
    }

    fun sendNowPlayingToServer(track: MediaDescriptionCompat) {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            put("trackId", track.mediaId)
            put("isPlaying", "true")
        }

        val mes = jsonObject.toString()
        Log.d(TAG, "sendNowPlayingToServer: $mes")
        webSocket.send(mes)
    }
    fun sendStoppedPlayingToServer() {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            //put("trackId", track.mediaId)
            put("isPlaying", "false")
        }

        val mes = jsonObject.toString()
        Log.d(TAG, "sendStoppedPlayingToServer: $mes")
        webSocket.send(mes)
    }

    fun sortTracks(sort: Sort) {
        trackSorting = sort
        when(sort){
            Sort.ID -> pendingTracks.sortBy { it.id }
            Sort.A_TO_Z -> pendingTracks.sortBy { it.name }
            Sort.NEWEST -> pendingTracks.sortByDescending { it.addedToLibrary }
            Sort.OLDEST -> pendingTracks.sortBy { it.addedToLibrary }
        }
    }

    suspend fun getTrackLinks(id: Int) : TrackLinkResponse {

        if(lastVerifiedTrack == id) {
            return lastFetchedLinks
        }

        return try {
            lastFetchedLinks = networkApi.getTrackLink(userToken, id)
            lastVerifiedTrack = id
            lastFetchedLinks
        } catch (e: Exception) {
            Log.d(TAG, "$e")
            TrackLinkResponse(" ", null)
        }
    }

    private fun readyPendingSources(tracks: List<Track>) {
        pendingConcatenatingMediaSource.clear()
        pendingMetadataList.clear()
        tracks.map{
            pendingConcatenatingMediaSource.addCustomMediaSource(it)
            pendingMetadataList.add(it.toMediaMetadataItem())
        }
    }


    suspend fun getAllTracks(): Flow<DataState<out List<Track>>> = flow {

        //if in memory, emit the memory
        if(!allTracks.isNullOrEmpty()) {
            Log.d(TAG, "getAllTracks: Retrieving Tracks From memory")
            pendingTracks.clear()
            pendingTracks.addAll(allTracks.values)
            sortTracks(trackSorting)
            readyPendingSources(pendingTracks)
            emit(DataState(pendingTracks, StateEvent.Success))

            return@flow
        }

        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchAllTracksFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            Log.d(TAG, "getAllTracks: Retrieving Tracks From database")
            allTracks.clear()
            pendingTracks.clear()
            localCollection.map {
                allTracks[it.id] = it
                pendingTracks.add(it)
            }
            sortTracks(trackSorting)
            readyPendingSources(pendingTracks)
            emit(DataState(pendingTracks, StateEvent.Success))
            return@flow
        }

        //else in network, fetch network, write to database and cache in memory, then emit memory
        Log.d(TAG, "getAllTracks: Retrieving Tracks From network")
        val remoteCollection = fetchAllTracksFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            remoteCollection.map {
                databaseDao.insertTrack(cacheMapper.mapToTrackEntity(it))
                allTracks[it.id] = it
                pendingTracks.add(it)
            }
            sortTracks(trackSorting)
            readyPendingSources(pendingTracks)
            emit(DataState(pendingTracks, StateEvent.Success))
            return@flow
        }

        //else unable to retrieve data
        emit(DataState(null, StateEvent.Error))
    }
    private suspend fun fetchAllTracksFromDatabase() : List<Track> {
        return when(sharedPreferences.getString(KEY_SORT, SORT_BY_ID)?.toSort()) {
            Sort.ID -> cacheMapper.mapFromTrackEntityList(databaseDao.getAllTracks())
            Sort.A_TO_Z -> cacheMapper.mapFromTrackEntityList(databaseDao.getAllTracksSortedAz())
            Sort.NEWEST -> cacheMapper.mapFromTrackEntityList(databaseDao.getAllTracksSortedDateAddedNewest())
            Sort.OLDEST -> cacheMapper.mapFromTrackEntityList(databaseDao.getAllTracksSortedDateAddedOldest())
            else -> cacheMapper.mapFromTrackEntityList(databaseDao.getAllTracks())
        }
    }
    private suspend fun fetchAllTracksFromNetwork() : List<Track> {
        return try{
             networkMapper.mapFromTrackEntityList(networkApi.get(userToken).trackList)
        } catch (e: Exception){
            Log.d(TAG, "$e")
            emptyList()
        }
    }

    suspend fun getAllUsers(): Flow<DataState<out List<User>>> = flow {

        //if in memory, emit the memory
        if(!allUsers.isNullOrEmpty()) {
            Log.d(TAG, "getAllTracks: Retrieving Users From memory")
            emit(DataState(allUsers, StateEvent.Success))

            return@flow
        }
        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchAllUsersFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            Log.d(TAG, "getAllUsers: Retrieving Users From database")
            allUsers.clear()
            localCollection.map {
                allUsers.add(it)
            }
            emit(DataState(allUsers, StateEvent.Success))
            return@flow
        }

        //else in network, fetch network, write to database and cache in memory, then emit memory
        Log.d(TAG, "getAllUsers: Retrieving Users from network")
        val remoteCollection = fetchAllUsersFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            allUsers.clear()
            remoteCollection.map {
                databaseDao.insertUser(cacheMapper.mapToUserEntity(it))
                allUsers.add(it)
            }
            emit(DataState(allUsers, StateEvent.Success))
            return@flow
        }

        //else unable to retrieve data
        emit(DataState(null, StateEvent.Error))
    }
    private suspend fun fetchAllUsersFromDatabase() : List<User> {
        return cacheMapper.mapFromUserEntityList(databaseDao.getAllUsers())
    }
    private suspend fun fetchAllUsersFromNetwork() : List<User> {
        return try{
            networkMapper.mapFromUserEntityList(networkApi.getAllUsers(userToken))
        } catch (e: Exception){
            Log.d(TAG, "$e")
            emptyList()
        }
    }

    suspend fun getAllPlaylistKeys(): Flow<DataState<out List<PlaylistKey>>> = flow {

        if(!playlistKeys.isNullOrEmpty()){
            Log.d(TAG, "getAllPlaylistKeys: Retrieving playlists From sorted list in memory")
            emit(DataState(playlistKeys, StateEvent.Success))

            return@flow
        }
        emit(DataState(null, StateEvent.Loading))

        val localCollection = fetchAllPlaylistKeysFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            Log.d(TAG, "getAllPlaylistKeys: Retrieving playlists From database")
            playlistKeys.clear()
            localCollection.map {
                playlistKeys.add(it)
            }
            emit(DataState(playlistKeys, StateEvent.Success))
            return@flow
        }

        Log.d(TAG, "getAllPlaylistKeys: Retrieving playlists from network")
        val remoteCollection = fetchAllPlaylistKeysFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            playlistKeys.clear()
            remoteCollection.map {
                databaseDao.insertPlaylistKey(cacheMapper.mapToPlaylistKeyEntity(it))
                playlistKeys.add(it)
            }

            emit(DataState(playlistKeys, StateEvent.Success))
            return@flow
        }

        emit(DataState(null, StateEvent.Error))
    }
    private suspend fun fetchAllPlaylistKeysFromDatabase() : List<PlaylistKey> {
        return cacheMapper.mapFromPlaylistEntityList(databaseDao.getAllPlaylists()).sortedBy { it.name }
    }
    private suspend fun fetchAllPlaylistKeysFromNetwork() : List<PlaylistKey> {
        return try{
            networkMapper.mapFromPlaylistKeyEntityList(networkApi.getAllPlaylists(userToken))
        } catch (e: Exception){
            Log.d(TAG, "$e")
            emptyList()
        }
    }

    suspend fun getPlaylist(playlistKeyId: Int): Flow<DataState<out Playlist>> = flow {

        val playlist = playlists.find { plist -> plist.id == playlistKeyId  }

        //if in memory, emit the memory
        if(playlist != null) {
            pendingTracks.clear()
            pendingTracks.addAll(playlist.playlistItems.map {
                it.track
            })
            readyPendingSources(pendingTracks)
            emit(DataState(playlist, StateEvent.Success))
            Log.d(TAG, "getPlaylist: Retrieved Playlist From memory")
            return@flow
        }

        val playlistKey = playlistKeys.find {playlistKeyId == it.id } ?: return@flow
        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchPlaylistFromDatabase(playlistKey)
        if(!localCollection.playlistItems.isNullOrEmpty()) {
            playlists.add(localCollection)
            pendingTracks.clear()
            pendingTracks.addAll(localCollection.playlistItems.map {
                it.track
            })
            readyPendingSources(pendingTracks)
            Log.d(TAG, "getPlaylist: Retrieved Playlist From database")
            emit(DataState(localCollection, StateEvent.Success))
            return@flow
        }

        //else in network, fetch network, write to database and cache in memory, then emit memory
        val remotePlaylist = fetchPlaylistFromNetwork(playlistKey)
        if(remotePlaylist != null) {
            cacheMapper.mapToPlaylistItemList(remotePlaylist).map {
                databaseDao.insertPlaylistReferenceData(it)
            }
            playlists.add(remotePlaylist)
            pendingTracks.clear()
            pendingTracks.addAll(remotePlaylist.playlistItems.map {
                it.track
            })
            readyPendingSources(pendingTracks)
            Log.d(TAG, "getPlaylist: Retrieved Playlist From network")
            emit(DataState(remotePlaylist, StateEvent.Success))
            return@flow
        }

        //else unable to retrieve data
        emit(DataState(null, StateEvent.Error))
    }
    private suspend fun fetchPlaylistFromDatabase(playlistKey: PlaylistKey): Playlist {
        val referenceDataList = databaseDao.getPlaylistReferenceData(playlistKey.id)

        val trackCacheEntityList = referenceDataList.map {
            databaseDao.getTrackById(it.trackId)
        }

        val trackList = cacheMapper.mapFromTrackEntityList(trackCacheEntityList)

        val trackMap = trackList.map { it.id to it}.toMap()

        val playlistItemList = referenceDataList.map {

            PlaylistItem(
                id = it.id,
                track = trackMap[it.trackId] ?: error("Lost it already somehow?"),
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }.sortedBy { it.id }

        return Playlist(
            id = playlistKey.id,
            name = playlistKey.name,
            playlistItems = playlistItemList,
            createdAt = playlistKey.createdAt,
            updatedAt = playlistKey.updatedAt

        )
    }
    private suspend fun fetchPlaylistFromNetwork(playlistKey: PlaylistKey): Playlist? {
        return try{
             val theList = networkMapper.mapToPlaylist(
                playlistKey,
                networkApi.getAllPlaylistTracks(userToken, playlistKey.id, "id,ASC", 1000)
            )
           theList
        } catch (e: Exception){
            Log.d(TAG, "$e")
            null
        }

    }


    suspend fun getToken(loginRequest: LoginRequest): Flow<SessionState<*>> = flow {
        emit(SessionState(null, StateEvent.Loading))
        try {
            val loginResponse = networkApi.getAuthorization(loginRequest)
            userToken = loginResponse.token

            sharedPreferences.edit()
                .putString(KEY_USER_TOKEN, userToken)
                .apply()

            initWebSocket()

            emit(SessionState(loginResponse, StateEvent.AuthSuccess))
        } catch(e: Exception) {
            emit(SessionState(null, StateEvent.Error))
        }
    }

    private fun initWebSocket() {
        if(userToken != "") {
            val request = Request.Builder()
                .url("wss://gorillagroove.net/api/socket")
                .addHeader("Authorization", "Bearer $userToken")
                .build()
            webSocket = okClient.newWebSocket(request, OkHttpWebSocket())
        }

    }

    fun cleanUpAndCloseConnections() {
        okClient.dispatcher.executorService.shutdown()
    }

    private fun ConcatenatingMediaSource.addCustomMediaSource(track: Track) {
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(dataSourceFactory, object: ResolvingDataSource.Resolver {
            var oldUri: Uri? = null
            var newUri: Uri? = null

            override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                if(dataSpec.uri == oldUri || dataSpec.uri == newUri) {
                    newUri?.let { return dataSpec.buildUpon().setUri(it).build() }
                }

                oldUri = dataSpec.uri
                lateinit var fetchedUri : Uri
                lateinit var fetchedUris: TrackLinkResponse
                runBlocking {
                    fetchedUris = getTrackLinks(Integer.parseInt(dataSpec.uri.toString()))
                }

                fetchedUri = Uri.parse(fetchedUris.trackLink)
                newUri = fetchedUri



                return dataSpec.buildUpon().setUri(fetchedUri).build()

            }
        })

        val progressiveMediaSource = ProgressiveMediaSource.Factory(resolvingDataSourceFactory)
        this.addMediaSource(progressiveMediaSource.createMediaSource(track.toMediaItem()))
    }
}

fun Track.toMediaMetadataItem(): MediaMetadataCompat =
    MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, id.toString())
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, length)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, name)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, album)
        .build()

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(id.toString())
        .build()

private fun String.toSort() : Sort {
    return when(this) {
        SORT_BY_ID -> Sort.ID
        SORT_BY_AZ -> Sort.A_TO_Z
        SORT_BY_DATE_ADDED_NEWEST -> Sort.NEWEST
        SORT_BY_DATE_ADDED_OLDEST -> Sort.OLDEST
        else -> Sort.ID
    }
}

//enum class Sort(i: Int) {ID(5), A_TO_Z, NEWEST, OLDEST}
enum class Sort {ID, A_TO_Z, NEWEST, OLDEST}