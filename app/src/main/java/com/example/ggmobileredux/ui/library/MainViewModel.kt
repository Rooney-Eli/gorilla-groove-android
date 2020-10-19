package com.example.ggmobileredux.ui.library

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.repository.Sort
import com.example.ggmobileredux.retrofit.LoginRequest
import com.example.ggmobileredux.service.EMPTY_PLAYBACK_STATE
import com.example.ggmobileredux.service.MusicServiceConnection
import com.example.ggmobileredux.util.DataState
import com.example.ggmobileredux.util.SessionState
import com.example.ggmobileredux.util.StateEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class MainViewModel
@ViewModelInject
constructor(
    private val mainRepository: MainRepository,
    musicServiceConnection: MusicServiceConnection,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "AppDebug"

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    //General App Data
    private val _loginState: MutableLiveData<SessionState<*>> = MutableLiveData()
    val loginState: LiveData<SessionState<*>>
        get() = _loginState

    private val _libraryTracks: MutableLiveData<DataState<*>> = MutableLiveData()
    val libraryTracks: LiveData<DataState<*>>
        get() = _libraryTracks

    private val _nowPlayingTracks: MutableLiveData<List<Track>> = MutableLiveData()
    val nowPlayingTracks: LiveData<List<Track>>
        get() = _nowPlayingTracks



    //Controls and Player Data
    private val _currentTrackItem: MutableLiveData<MediaMetadataCompat> = MutableLiveData()
    val currentTrackItem: LiveData<MediaMetadataCompat>
        get() = _currentTrackItem

    private val _playPauseState: MutableLiveData<PlaybackStateCompat> = MutableLiveData()
    val playPauseState: LiveData<PlaybackStateCompat>
        get() = _playPauseState

    val mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }


    @ExperimentalCoroutinesApi
    fun setLibraryEvent(libraryEvent: LibraryEvent<Int>) {
        viewModelScope.launch {
            when (libraryEvent) {
                is LibraryEvent.GetAllTracksEvents -> {
                    mainRepository.getAllTracks()
                        .onEach {
                            _libraryTracks.value = it
                        }
                        .launchIn(viewModelScope)
                }
                is LibraryEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setLoginStateEvent(loginStateEvent: LoginStateEvent<LoginRequest>) {
        viewModelScope.launch {
            when (loginStateEvent) {
                is LoginStateEvent.LoginEvent<LoginRequest> -> {
                    mainRepository.getToken(loginStateEvent.data)
                        .onEach {
                            _loginState.value = it
                        }
                        .launchIn(viewModelScope)
                }
                is LoginStateEvent.None -> {
                    //ignored
                }
            }
        }
    }


    fun getNowPlayingTracks() {
        _nowPlayingTracks.value = mainRepository.fetchNowPlayingTracks()
    }

    fun setNowPlayingTracks(trackIds: List<Int> ) {
        mainRepository.setNowPlayingTracks(trackIds)
        _nowPlayingTracks.value = mainRepository.fetchNowPlayingTracks().also { playMedia(it[0]) }
    }

    fun sortTracks(sort: Sort) {
        mainRepository.sortTracks(sort)
        _libraryTracks.value = DataState(mainRepository.sortedTrackList, StateEvent.Success)
    }

    fun playMedia(track: Track) {
        val transportControls = musicServiceConnection.transportControls
        transportControls.playFromMediaId(track.id.toString(), null)
    }

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE

        _playPauseState.postValue(playbackState)
        Log.d(TAG, "${it.state}")

        when(it.state){
            STATE_PLAYING -> {
                //notify GG of track and user is listening
            }
            STATE_STOPPED -> {
                //notify GG
            }
        }
    }

    private val mediaMetadataObserver = Observer<MediaMetadataCompat> { mediaMetadataCompat ->
        _currentTrackItem.postValue(mediaMetadataCompat)
    }

    private val musicServiceConnection = musicServiceConnection.also {

        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
        checkPlaybackPosition()
    }

    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        val currPosition = _playPauseState.value?.currentPlayBackPosition
        if (mediaPosition.value != currPosition)
            mediaPosition.postValue(currPosition)
        if (updatePosition)
            checkPlaybackPosition()
    }, POSITION_UPDATE_INTERVAL_MILLIS)

    fun playPause() {
        musicServiceConnection.playbackState.value?.let {
            if(it.isPaused) {
                Log.d(TAG, "playPause: playing...")
                musicServiceConnection.transportControls.play()
                _playPauseState.postValue(it)
            }
            else {
                Log.d(TAG, "playPause: pausing...")
                musicServiceConnection.transportControls.pause()
                _playPauseState.postValue(it)
            }
        }
    }

    fun skipTo(position: Long) {
        musicServiceConnection.transportControls.seekTo(position)

    }


    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)
        updatePosition = false
    }
}

private const val POSITION_UPDATE_INTERVAL_MILLIS = 1000L

inline val PlaybackStateCompat.isPrepared
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING) ||
            (state == PlaybackStateCompat.STATE_PAUSED)

inline val PlaybackStateCompat.isPaused
    get() = (state == PlaybackStateCompat.STATE_PAUSED)

inline val PlaybackStateCompat.isPlaying
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING)

inline val PlaybackStateCompat.isPlayEnabled
    get() = (actions and PlaybackStateCompat.ACTION_PLAY != 0L) ||
            ((actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) &&
                    (state == PlaybackStateCompat.STATE_PAUSED))

inline val PlaybackStateCompat.isPauseEnabled
    get() = (actions and PlaybackStateCompat.ACTION_PAUSE != 0L) ||
            ((actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) &&
                    (state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_PLAYING))

inline val PlaybackStateCompat.currentPlayBackPosition: Long
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDelta * playbackSpeed)).toLong()
    } else {
        position
    }


sealed class LoginStateEvent<out R> {
    data class LoginEvent<out T>(val data: T): LoginStateEvent<T>()
    object None: LoginStateEvent<Nothing>()
}

sealed class LibraryEvent<out R> {
    object GetAllTracksEvents: LibraryEvent<Nothing>()
    object None: LibraryEvent<Nothing>()
}