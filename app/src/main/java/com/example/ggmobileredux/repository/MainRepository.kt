package com.example.ggmobileredux.repository

import android.content.SharedPreferences
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.core.net.toUri
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.retrofit.LoginRequest
import com.example.ggmobileredux.retrofit.NetworkMapper
import com.example.ggmobileredux.retrofit.TrackRetrofit
import com.example.ggmobileredux.room.CacheMapper
import com.example.ggmobileredux.room.TrackDao
import com.example.ggmobileredux.util.Constants
import com.example.ggmobileredux.util.Constants.KEY_SORT
import com.example.ggmobileredux.util.Constants.KEY_USER_TOKEN
import com.example.ggmobileredux.util.Constants.SORT_BY_AZ
import com.example.ggmobileredux.util.Constants.SORT_BY_DATE_ADDED_NEWEST
import com.example.ggmobileredux.util.Constants.SORT_BY_DATE_ADDED_OLDEST
import com.example.ggmobileredux.util.Constants.SORT_BY_ID
import com.example.ggmobileredux.util.DataState
import com.example.ggmobileredux.util.SessionState
import com.example.ggmobileredux.util.StateEvent
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import java.io.IOException

class MainRepository
constructor(
    private val trackDao: TrackDao,
    private val trackRetrofit: TrackRetrofit,
    private val cacheMapper: CacheMapper,
    private val networkMapper: NetworkMapper,
    private val sharedPreferences: SharedPreferences,
    private val dataSourceFactory: DefaultDataSourceFactory
) {
    private val TAG = "AppDebug: Repository"


    var userToken: String = sharedPreferences.getString(KEY_USER_TOKEN, "") ?: ""
    //var sortPref: String = sharedPreferences.getString(KEY_SORT, "") ?: ""

    val concatenatingMediaSource = ConcatenatingMediaSource()

    val recentSongs = mutableListOf<MediaMetadataCompat>()

    fun getLoadedSongById(id: Int) : MediaMetadataCompat? {
        return recentSongs.find {  Integer.parseInt(it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) == id  }
    }

    suspend fun getAllTracks(): Flow<DataState<*>> = flow {
        emit(DataState(null, StateEvent.Loading))
        Log.d(TAG, "getAllTracks: Getting Tracks from local database...")
        val localCollection = when(sharedPreferences.getString(KEY_SORT, "")) {
            SORT_BY_AZ -> {
                Log.d(TAG, "getAllTracks: Fetching alphabetically...")
                trackDao.getAllTracksSortedAz()
            }
            SORT_BY_ID -> {
                trackDao.getAllTracks()
            }
            SORT_BY_DATE_ADDED_OLDEST -> {
                trackDao.getAllTracksSortedDateAddedOldest()
            }
            SORT_BY_DATE_ADDED_NEWEST -> {
                trackDao.getAllTracksSortedDateAddedNewest()
            }
            else -> {
                Log.d(TAG, "getAllTracks: Sorting Error!!!!?")
                trackDao.getAllTracks()
            }
        }

        Log.d(TAG, "getAllTracks: Sort by value: ")
        //localCollection = trackDao.getAllTracks()

        if(!localCollection.isNullOrEmpty()) {
            Log.d(TAG, "getAllTracks: returning ${localCollection.size} tracks from local database...")
            emit(DataState(cacheMapper.mapFromEntityList(localCollection), StateEvent.Success))
            return@flow
        }
        Log.d(TAG, "getAllTracks: No tracks in local database.")
        try {

            Log.d(TAG, "getAllTracks: Attempting to get tracks from remote source...")

            val netTracksWrapper = trackRetrofit.get(userToken)
            val tracks = networkMapper.mapFromTrackEntityList(netTracksWrapper.trackList)

            Log.d(TAG, "getAllTracks: Successfully retrieved tracks from remote source!")
            Log.d(TAG, "getAllTracks: Caching tracks in local database...")
            cacheMapper.mapToEntityList(tracks).map { trackDao.insert(it) }
            Log.d(TAG, "getAllTracks: Tracks cached!")

            emit(DataState(tracks, StateEvent.Success))

        } catch(e: Exception) {
            Log.d(TAG, "getAllTracks: Error retrieving tracks from remote source: $e")
            emit(DataState(null, StateEvent.Error))
        }
    }

    suspend fun getTrackWithLink(trackId: Int): Flow<DataState<*>> = flow {
        emit(DataState(null, StateEvent.Loading))
        try {
            Log.d(TAG, "getTrack: Attepting to get track from remote source...")
            val track = trackRetrofit.getTrack(userToken, trackId)
            val links = trackRetrofit.getTrackLink(userToken, track.id)


            //This validation code exists because GG backend returns a link to a 404'd page if there
            //is no art set for the track and I can't figure out how to make Glide happy about it
            Log.d(TAG, "getTrackWithLink: Validating album art link...")
            var isValid = false
            runBlocking {
                withContext(Dispatchers.IO) {
                    isValid = validateLink(links.albumArtLink.toString())
                }

            }
            when(isValid) {
                true ->{
                    Log.d(TAG, "getTrackWithLink: Album art link is valid!")
                    track.albumArtLink = links.albumArtLink
                }
                false -> {
                    Log.d(TAG, "getTrackWithLink: Album art link is NOT valid!")
                    track.albumArtLink = null
                }
            }
            track.trackLink = links.trackLink

            Log.d(TAG, "getTrack: Successfully retrieved track from remote source!")
            recentSongs.add(networkMapper.mapFromTrackEntity(track).toMediaMetadataItem())

            val x = networkMapper.mapFromTrackEntity(track)
            emit(DataState(x, StateEvent.TrackSuccess))

        } catch(e: Exception) {
            Log.d(TAG, "getTrack: Error retrieving tracks from remote source: ${e.toString()}")
            emit(DataState(null, StateEvent.Error))
        }
    }

    private fun validateLink(url: String): Boolean {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.d(TAG, "onFailure: album link IS NOT VALID")
//            }
//            override fun onResponse(call: Call, response: Response) {
//
//            }
//        })
        val call = client.newCall(request)
        val response = call.execute()
        Log.d(TAG, "validateLink: server response: ${response.code()}")
        return response.code() != 404
    }


    suspend fun getTrackLink(id: Int): Flow<DataState<*>> = flow {
        emit(DataState(null, StateEvent.Loading))
        try {

            val responseObject = trackRetrofit.getTrackLink(userToken, id)
            emit(DataState(responseObject, StateEvent.TrackSuccess))

        } catch (e: Exception) {
            Log.d("AppDebug", "getTrackLink: crashed" + e.toString())
            emit(DataState(null, StateEvent.Error))
        }

    }

    fun sortSongsAz() {

    }


    suspend fun getToken(loginRequest: LoginRequest): Flow<SessionState<*>> = flow {
        emit(SessionState(null, StateEvent.Loading))
        try {
            val loginResponse = trackRetrofit.getAuthorization(loginRequest)
            userToken = loginResponse.token
            writePersonalDataToSharedPref(userToken)
            emit(SessionState(loginResponse, StateEvent.AuthSuccess))
        } catch(e: Exception) {
            emit(SessionState(null, StateEvent.Error))
        }
    }

    private fun writePersonalDataToSharedPref(token: String) {

        sharedPreferences.edit()
            .putString(Constants.KEY_USER_TOKEN, token)
            .apply()

    }

    fun addMediaSource(track: MediaMetadataCompat) {
        concatenatingMediaSource.addMediaSource(
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI).toUri())
        )
    }

    suspend fun getLink(id: Int): String {
        try {
            Log.d(TAG, "getLink: Attempting to get track link from remote source...")
            val link = trackRetrofit.getTrackLink(userToken, id).trackLink
            Log.d(TAG, "getLink: Successfully retrieved track link from remote source!")
            return link

        } catch(e: Exception) {
            Log.d(TAG, "getLink: Failed to retrieve track link from remote source: $e")
            return "no link"
        }
    }

    private fun Track.toMediaMetadataItem(): MediaMetadataCompat =
        MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, albumArtLink)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, trackLink.toString())
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArtLink)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, length)

            //display properties, makes it "easier" to display
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, name)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, album)
            .build()
}

interface NetworkListener {
    fun onFailure(request: Request?, e: IOException?)
    fun onResponse(response: Response?)
}
