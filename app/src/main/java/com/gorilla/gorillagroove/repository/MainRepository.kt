package com.gorilla.gorillagroove.repository

import android.content.SharedPreferences
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.gorilla.gorillagroove.network.*
import com.gorilla.gorillagroove.database.CacheMapper
import com.gorilla.gorillagroove.database.DatabaseDao
import com.gorilla.gorillagroove.model.*
import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.track.TrackLinkResponse
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.util.Constants.CALLING_FRAGMENT_LIBRARY
import com.gorilla.gorillagroove.util.Constants.CALLING_FRAGMENT_PLAYLIST
import com.gorilla.gorillagroove.util.Constants.KEY_FIRST_TIME_TOGGLE
import com.gorilla.gorillagroove.util.Constants.KEY_SORT
import com.gorilla.gorillagroove.util.Constants.KEY_USER_TOKEN
import com.gorilla.gorillagroove.util.Constants.SORT_BY_AZ
import com.gorilla.gorillagroove.util.Constants.SORT_BY_DATE_ADDED_NEWEST
import com.gorilla.gorillagroove.util.Constants.SORT_BY_DATE_ADDED_OLDEST
import com.gorilla.gorillagroove.util.Constants.SORT_BY_ID
import com.gorilla.gorillagroove.util.DataState
import com.gorilla.gorillagroove.util.SessionState
import com.gorilla.gorillagroove.util.StateEvent
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.gorilla.gorillagroove.util.Constants.SORT_BY_ARTIST_AZ
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

    private var lastVerifiedTrack: Long? = null
    private lateinit var lastFetchedLinks: TrackLinkResponse

    //glorified memory for lookups
    private val allTracks = LinkedHashMap<Long, Track>()
    private val allUsers = mutableListOf<User>()
    private val playlistKeys = mutableListOf<PlaylistKey>()
    private val playlists = mutableListOf<Playlist>()


    init {
        initWebSocket()
    }

    var dataSetChanged = false
    var currentIndex = 0


    val allLibraryTracks = mutableListOf<Track>()
    val libraryConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val libraryMetadataList = mutableListOf<MediaMetadataCompat>()

    val playlistConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val playlistMetadataList = mutableListOf<MediaMetadataCompat>()

    //This is directly tied to Now Playing Fragment display
    val nowPlayingTracks = mutableListOf<Track>()
    val nowPlayingConcatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val nowPlayingMetadataList = mutableListOf<MediaMetadataCompat>()

    fun changeMediaSource(callingFragment: String, playlistId: Long?) {
        when(callingFragment) {
            CALLING_FRAGMENT_LIBRARY -> {
                dataSetChanged = true
                nowPlayingTracks.clear()
                nowPlayingConcatenatingMediaSource.clear()
                nowPlayingMetadataList.clear()

                nowPlayingTracks.addAll(allTracks.values.toList())
                nowPlayingTracks.sort(trackSorting)
                nowPlayingTracks.map {
                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
                }
            }
            CALLING_FRAGMENT_PLAYLIST -> {
                dataSetChanged = true
                nowPlayingTracks.clear()
                nowPlayingConcatenatingMediaSource.clear()
                nowPlayingMetadataList.clear()

                val playlistItems = playlists.find { pId -> pId.id == playlistId }?.playlistItems

                playlistItems?.map { it.track }?.let { nowPlayingTracks.addAll(it) }
                nowPlayingTracks.map {
                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
                }
            }
        }
    }

    private fun insertNowPlayingTrack(track: Track) {
        if(nowPlayingTracks.size > 0) {
            nowPlayingTracks.add(currentIndex + 1, track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(currentIndex+1, track)
            nowPlayingMetadataList.add(currentIndex +1, track.toMediaMetadataItem())
        } else {
            dataSetChanged = true
            nowPlayingTracks.add(currentIndex, track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(currentIndex, track)
            nowPlayingMetadataList.add(currentIndex, track.toMediaMetadataItem())
        }
    }
    private fun addToEndNowPlayingTrack(track: Track) {
        if(nowPlayingTracks.size > 0) {
            nowPlayingTracks.add(track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(track)
            nowPlayingMetadataList.add(track.toMediaMetadataItem())
        } else {
            dataSetChanged = true
            nowPlayingTracks.add(track)
            nowPlayingConcatenatingMediaSource.addCustomMediaSource(track)
            nowPlayingMetadataList.add(track.toMediaMetadataItem())
        }
    }


    fun sendNowPlayingToServer(track: MediaDescriptionCompat) {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            put("trackId", track.mediaId)
            put("isPlaying", "true")
        }

        val mes = jsonObject.toString()
        //Log.d(TAG, "sendNowPlayingToServer: $mes")
        webSocket.send(mes)
    }
    fun sendStoppedPlayingToServer() {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            //put("trackId", track.mediaId)
            put("isPlaying", "false")
        }

        val mes = jsonObject.toString()
        //Log.d(TAG, "sendStoppedPlayingToServer: $mes")
        webSocket.send(mes)
    }




    fun setSelectedTracks(trackIds: List<Long>, selectionOperation: SelectionOperation) {
        when(selectionOperation) {
            SelectionOperation.PLAY_NOW -> {
                dataSetChanged = true
                nowPlayingTracks.clear()
                nowPlayingConcatenatingMediaSource.clear()
                nowPlayingMetadataList.clear()

                trackIds.map { allTracks[it]?.let { track -> nowPlayingTracks.add(track) } }
                nowPlayingTracks.map {
                    nowPlayingConcatenatingMediaSource.addCustomMediaSource(it)
                    nowPlayingMetadataList.add(it.toMediaMetadataItem())
                }


            }
            SelectionOperation.PLAY_NEXT -> {
                trackIds.asReversed().map { allTracks[it]?.let { track ->
                        insertNowPlayingTrack(track)
                    }
                }
            }
            SelectionOperation.PLAY_LAST -> {
                    trackIds.asReversed().map { allTracks[it]?.let { track ->
                        addToEndNowPlayingTrack(track)
                    }
                }
            }
        }

    }



    suspend fun getTrackLinks(id: Long) : TrackLinkResponse {

        if(lastVerifiedTrack == id) {
            return lastFetchedLinks
        }

        return try {
            lastFetchedLinks = networkApi.getTrackLink(userToken, id)
            lastVerifiedTrack = id
            lastFetchedLinks
        } catch (e: Exception) {
            //Log.d(TAG, "$e")
            TrackLinkResponse(" ", null)
        }
    }

    private fun readyLibrarySources(tracks: List<Track>) {
        libraryConcatenatingMediaSource.clear()
        libraryMetadataList.clear()
        tracks.map{
            libraryConcatenatingMediaSource.addCustomMediaSource(it)
            libraryMetadataList.add(it.toMediaMetadataItem())
        }
    }

    private fun readyPlaylistSources(tracks: List<Track>) {
        playlistConcatenatingMediaSource.clear()
        playlistMetadataList.clear()
        tracks.map{
            playlistConcatenatingMediaSource.addCustomMediaSource(it)
            playlistMetadataList.add(it.toMediaMetadataItem())
        }
    }

    suspend fun getNowPlayingTracks(): Flow<DataState<out List<Track>>> = flow {
        emit(DataState(nowPlayingTracks, StateEvent.Success))
    }

    fun getTrack(trackId: Long): Flow<DataState<out Track>> = flow {
        emit(DataState(allTracks[trackId], StateEvent.Success))
    }

    suspend fun updateAllTracks(): Flow<DataState<out List<Track>>> = flow {
        val remoteCollection = fetchAllTracksFromNetwork()
        val diff = remoteCollection.filterNot { allLibraryTracks.contains(it) }
        diff.map {
            databaseDao.insertTrack(cacheMapper.mapToTrackEntity(it))
            allTracks[it.id] = it
            allLibraryTracks.add(it)
        }
        sortLibrary(trackSorting)
        readyLibrarySources(allLibraryTracks)
        emit(DataState(allLibraryTracks, StateEvent.Success))
    }

    suspend fun getAllTracks(): Flow<DataState<out List<Track>>> = flow {

        if(!allLibraryTracks.isNullOrEmpty()) {
            //Log.d(TAG, "getAllTracks: Retrieving Tracks From memory")
            //sortTracks(trackSorting)
            readyLibrarySources(allLibraryTracks)
            emit(DataState(allLibraryTracks, StateEvent.Success))

            return@flow
        }

        //if in memory, emit the memory
        if(!allTracks.isNullOrEmpty()) {
            //Log.d(TAG, "getAllTracks: Retrieving Tracks From memory")
            //sortTracks(trackSorting)
            readyLibrarySources(allLibraryTracks)
            emit(DataState(allLibraryTracks, StateEvent.Success))

            return@flow
        }

        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchAllTracksFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            //Log.d(TAG, "getAllTracks: Retrieving Tracks From database")
            allTracks.clear()
            localCollection.map {
                allTracks[it.id] = it
                allLibraryTracks.add(it)
            }
            //sortTracks(trackSorting)
            readyLibrarySources(allTracks.values.toList())
            emit(DataState(allTracks.values.toList(), StateEvent.Success))
            return@flow
        }

        //else in network, fetch network, write to database and cache in memory, then emit memory
        //Log.d(TAG, "getAllTracks: Retrieving Tracks From network")
        val remoteCollection = fetchAllTracksFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            remoteCollection.map {
                databaseDao.insertTrack(cacheMapper.mapToTrackEntity(it))
                allTracks[it.id] = it
                allLibraryTracks.add(it)
            }
//            sortTracks(trackSorting)
            readyLibrarySources(allTracks.values.toList())
            emit(DataState(allLibraryTracks, StateEvent.Success))
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
            val list = networkApi.get(userToken).trackList
             networkMapper.mapFromTrackEntityList(list)
        } catch (e: Exception){
            //Log.d(TAG, "$e")
            emptyList()
        }
    }

    suspend fun getAllUsers(): Flow<DataState<out List<User>>> = flow {

        //if in memory, emit the memory
        if(!allUsers.isNullOrEmpty()) {
            //Log.d(TAG, "getAllTracks: Retrieving Users From memory")
            emit(DataState(allUsers, StateEvent.Success))

            return@flow
        }
        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchAllUsersFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            //Log.d(TAG, "getAllUsers: Retrieving Users From database")
            allUsers.clear()
            localCollection.map {
                allUsers.add(it)
            }
            emit(DataState(allUsers, StateEvent.Success))
            return@flow
        }

        //else in network, fetch network, write to database and cache in memory, then emit memory
        //Log.d(TAG, "getAllUsers: Retrieving Users from network")
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
            //Log.d(TAG, "$e")
            emptyList()
        }
    }



    suspend fun updateAllPlaylists(): Flow<DataState<out List<PlaylistKey>>> = flow {
        val remoteCollection = fetchAllPlaylistKeysFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            playlistKeys.clear()
            playlists.clear()
            databaseDao.deleteAllPlaylistData()
            databaseDao.deleteAllPlaylists()
            remoteCollection.map {
                databaseDao.insertPlaylistKey(cacheMapper.mapToPlaylistKeyEntity(it))
                playlistKeys.add(it)
            }

            emit(DataState(playlistKeys, StateEvent.Success))
            return@flow
        }
        emit(DataState(null, StateEvent.Error))
    }

    suspend fun getAllPlaylistKeys(): Flow<DataState<out List<PlaylistKey>>> = flow {

        if(!playlistKeys.isNullOrEmpty()){
            //Log.d(TAG, "getAllPlaylistKeys: Retrieving playlists From sorted list in memory")
            emit(DataState(playlistKeys, StateEvent.Success))

            return@flow
        }
        emit(DataState(null, StateEvent.Loading))

        val localCollection = fetchAllPlaylistKeysFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            //Log.d(TAG, "getAllPlaylistKeys: Retrieving playlists From database")
            playlistKeys.clear()
            localCollection.map {
                playlistKeys.add(it)
            }
            emit(DataState(playlistKeys, StateEvent.Success))
            return@flow
        }

        //Log.d(TAG, "getAllPlaylistKeys: Retrieving playlists from network")
        val remoteCollection = fetchAllPlaylistKeysFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            playlistKeys.clear()
            playlists.clear()
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
            //Log.d(TAG, "$e")
            emptyList()
        }
    }

    suspend fun getPlaylist(playlistKeyId: Long): Flow<DataState<out Playlist>> = flow {

        val playlist = playlists.find { plist -> plist.id == playlistKeyId  }

        //if in memory, emit the memory
        if(playlist != null) {
            readyPlaylistSources(playlist.playlistItems.map { it.track })
            emit(DataState(playlist, StateEvent.Success))
            //Log.d(TAG, "getPlaylist: Retrieved Playlist From memory")
            return@flow
        }

        val playlistKey = playlistKeys.find {playlistKeyId == it.id } ?: return@flow
        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchPlaylistFromDatabase(playlistKey)
        if(!localCollection.playlistItems.isNullOrEmpty()) {
            playlists.add(localCollection)
            readyPlaylistSources(localCollection.playlistItems.map { it.track })
            //Log.d(TAG, "getPlaylist: Retrieved Playlist From database")
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
            readyPlaylistSources(remotePlaylist.playlistItems.map { it.track })
            //Log.d(TAG, "getPlaylist: Retrieved Playlist From network")
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
            //Log.d(TAG, "$e")
            null
        }

    }

    suspend fun updateTrack(trackUpdate: TrackUpdate) : Flow<DataState<*>> = flow {
        emit(DataState(null, StateEvent.Loading))
        try {

            networkApi.updateTrack(userToken, trackUpdate)
            val updatedTrack = networkMapper.mapFromTrackEntity(networkApi.getTrack(userToken, trackUpdate.trackIds[0]))
            val oldTrack = allTracks[updatedTrack.id]
            allTracks[updatedTrack.id] = updatedTrack
            allLibraryTracks[allLibraryTracks.indexOf(oldTrack)] = updatedTrack
            databaseDao.updateTrack(cacheMapper.mapToTrackEntity(updatedTrack))

            emit(DataState(null, StateEvent.Success))
        } catch(e: Exception) {
            emit(DataState(null, StateEvent.Error))
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

    fun logoutUser() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_TIME_TOGGLE, true)
            .apply()
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
                    fetchedUris = getTrackLinks(Integer.parseInt(dataSpec.uri.toString()).toLong())
                }

                fetchedUri = Uri.parse(fetchedUris.trackLink)
                newUri = fetchedUri



                return dataSpec.buildUpon().setUri(fetchedUri).build()

            }
        })

        val progressiveMediaSource = ProgressiveMediaSource.Factory(resolvingDataSourceFactory)
        this.addMediaSource(progressiveMediaSource.createMediaSource(track.toMediaItem()))
    }

    private fun ConcatenatingMediaSource.addCustomMediaSource(index: Int, track: Track) {
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
                    fetchedUris = getTrackLinks(Integer.parseInt(dataSpec.uri.toString()).toLong())
                }

                fetchedUri = Uri.parse(fetchedUris.trackLink)
                newUri = fetchedUri



                return dataSpec.buildUpon().setUri(fetchedUri).build()

            }
        })

        val progressiveMediaSource = ProgressiveMediaSource.Factory(resolvingDataSourceFactory)
        this.addMediaSource(index, progressiveMediaSource.createMediaSource(track.toMediaItem()))
    }

    fun sortLibrary(sorting: Sort) {
        trackSorting = sorting
        when(sorting) {
            Sort.ID -> allLibraryTracks.sortBy { it.id }
            Sort.A_TO_Z -> allLibraryTracks.sortBy { it.name }
            Sort.NEWEST -> allLibraryTracks.sortByDescending { it.addedToLibrary }
            Sort.OLDEST -> allLibraryTracks.sortBy { it.addedToLibrary }
            Sort.ARTIST_A_TO_Z -> allLibraryTracks.sortBy { it.artist }
        }
    }

    fun MutableList<Track>.sort(sorting: Sort) {
        when(sorting) {
            Sort.ID -> this.sortBy { it.id }
            Sort.A_TO_Z -> this.sortBy { it.name }
            Sort.NEWEST -> this.sortByDescending { it.addedToLibrary }
            Sort.OLDEST -> this.sortBy { it.addedToLibrary }
        }
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
        SORT_BY_ARTIST_AZ -> Sort.ARTIST_A_TO_Z
        else -> Sort.ID
    }
}



//enum class Sort(i: Int) {ID(5), A_TO_Z, NEWEST, OLDEST}
enum class Sort {ID, A_TO_Z, NEWEST, OLDEST, ARTIST_A_TO_Z }
enum class SelectionOperation {PLAY_NOW, PLAY_NEXT, PLAY_LAST}