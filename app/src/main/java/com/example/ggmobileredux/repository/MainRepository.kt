package com.example.ggmobileredux.repository

import android.content.SharedPreferences
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.ggmobileredux.di.AppModule
import com.example.ggmobileredux.di.RetrofitModule
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.retrofit.*
import com.example.ggmobileredux.room.CacheMapper
import com.example.ggmobileredux.room.TrackDao
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
import dagger.Provides
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton


class MainRepository
constructor(
    private val trackDao: TrackDao,
    private val trackRetrofit: TrackRetrofit,
    private val cacheMapper: CacheMapper,
    private val networkMapper: NetworkMapper,
    private val sharedPreferences: SharedPreferences,
    private val dataSourceFactory: DefaultDataSourceFactory,
    private val okClient: OkHttpClient
) {
    private val TAG = "AppDebug: Repository"

    lateinit var webSocket: WebSocket

    private var userToken: String = sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""

    var trackSorting: Sort = sharedPreferences.getString(KEY_SORT, SORT_BY_ID)?.toSort() ?: Sort.ID

    private val allTracks = LinkedHashMap<Int, Track>() //glorified memory for lookups

    init {
        initWebSocket()
    }

    //KEEP THESE IN SYNC WITH EACH OTHER
    val concatenatingMediaSource = ConcatenatingMediaSource(false, true, ShuffleOrder.DefaultShuffleOrder(0))
    val metadataList = mutableListOf<MediaMetadataCompat>()


    val sortedTrackList = mutableListOf<Track>()
    private val nowPlayingTracks = mutableListOf<Track>()

    fun fetchNowPlayingTracks() : List<Track>{
        if(nowPlayingTracks.isNullOrEmpty()) {
            nowPlayingTracks.addAll(sortedTrackList)
        }
        return nowPlayingTracks
    }

    fun setNowPlayingTracks(trackIds: List<Int>) {
        nowPlayingTracks.clear()
        trackIds.map { nowPlayingTracks.add(allTracks[it]!!) }
        readySources(nowPlayingTracks)
    }


    fun sendNowPlayingToServer(track: MediaDescriptionCompat) {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            put("trackId", track.mediaId)
            put("isPlaying", "true")
        }

        val mes = jsonObject.toString()
        webSocket.send(mes)
    }

    fun sendStoppedPlayingToServer(track: MediaDescriptionCompat) {
        val jsonObject = JSONObject().apply {
            put("messageType", "NOW_PLAYING")
            put("trackId", track.mediaId)
            put("isPlaying", "false")
        }

        val mes = jsonObject.toString()
        webSocket.send(mes)
    }


    fun sortTracks(sort: Sort) {
        trackSorting = sort
        when(sort){
            Sort.ID -> sortedTrackList.sortBy { it.id }
            Sort.A_TO_Z -> sortedTrackList.sortBy { it.name }
            Sort.NEWEST -> sortedTrackList.sortByDescending { it.addedToLibrary }
            Sort.OLDEST -> sortedTrackList.sortBy { it.addedToLibrary }
        }

        readySources(sortedTrackList)
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

    var lastVerifiedTrack: Int? = null
    lateinit var lastFetchedLinks: TrackLinkResponse

    suspend fun getTrackLinks(id: Int) : TrackLinkResponse {

        if(lastVerifiedTrack == id) {
            return lastFetchedLinks
        }

        return try {
            val links = trackRetrofit.getTrackLink(userToken, id)
            val trackLink = links.trackLink
            var artLink = links.albumArtLink

            var isValid = false
            runBlocking {
                isValid = validateLink(links.albumArtLink.toString())
            }

            if(!isValid){
                Log.d(TAG, "getTrackWithLink: Album art link is NOT valid!")
                artLink = null

            }
            lastVerifiedTrack = id
            lastFetchedLinks = TrackLinkResponse(trackLink, artLink)
            return lastFetchedLinks

        } catch (e: Exception) {
            Log.d(TAG, "$e")
            return TrackLinkResponse(" ", null)
        }
    }

    fun readySources(playlist: List<Track>) {
        concatenatingMediaSource.clear()
        metadataList.clear()
        playlist.forEach{
            concatenatingMediaSource.addCustomMediaSource(it)
            metadataList.add(it.toMediaMetadataItem())
        }
    }


    suspend fun getAllTracks(): Flow<DataState<*>> = flow {

        if(!sortedTrackList.isNullOrEmpty()){
            Log.d(TAG, "getAllTracks: Retrieving Tracks From sorted list in memory")
            emit(DataState(sortedTrackList, StateEvent.Success))

            return@flow
        }

        //if in memory, emit the memory
        if(!allTracks.isNullOrEmpty()) {
            Log.d(TAG, "getAllTracks: Retrieving Tracks From memory")
            emit(DataState(allTracks.values.toList(), StateEvent.Success))

            return@flow
        }

        emit(DataState(null, StateEvent.Loading))

        //else in database, fetch db, cache to memory and emit memory
        val localCollection = fetchAllTracksFromDatabase()
        if(!localCollection.isNullOrEmpty()) {
            Log.d(TAG, "getAllTracks: Retrieving Tracks From database")
            allTracks.clear()
            localCollection.map {
                allTracks.put(it.id, it)
            }
            sortedTrackList.clear()
            sortedTrackList.addAll(localCollection)
            readySources(sortedTrackList)
            emit(DataState(allTracks.values.toList(), StateEvent.Success))
            return@flow
        }

        //else in network, fetch network, write to database and cache in memory, then emit memory
        Log.d(TAG, "getAllTracks: Retrieving Tracks From network")
        val remoteCollection = fetchAllTracksFromNetwork()
        if(!remoteCollection.isNullOrEmpty()) {
            remoteCollection.map {
                trackDao.insert(cacheMapper.mapToEntity(it))
                allTracks.put(it.id, it)
            }
            sortedTrackList.clear()
            sortedTrackList.addAll(remoteCollection)
            readySources(sortedTrackList)
            emit(DataState(allTracks.values.toList(), StateEvent.Success))
            return@flow
        }

        //else unable to retrieve data
        emit(DataState(null, StateEvent.Error))
    }

    private suspend fun fetchAllTracksFromDatabase() : List<Track> {
        return cacheMapper.mapFromEntityList(trackDao.getAllTracks())
    }

    private suspend fun fetchAllTracksFromNetwork() : List<Track> {
        return try{
             networkMapper.mapFromTrackEntityList(trackRetrofit.get(userToken).trackList)
        } catch (e: Exception){
            Log.d(TAG, "$e")
            emptyList()
        }
    }


    private fun validateLink(url: String): Boolean {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()
        val call = client.newCall(request)
        val response = call.execute()
        Log.d(TAG, "validateLink: server response: ${response.code}")
        return response.code != 404
    }

    suspend fun getToken(loginRequest: LoginRequest): Flow<SessionState<*>> = flow {
        emit(SessionState(null, StateEvent.Loading))
        try {
            val loginResponse = trackRetrofit.getAuthorization(loginRequest)
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

enum class Sort {ID, A_TO_Z, NEWEST, OLDEST}