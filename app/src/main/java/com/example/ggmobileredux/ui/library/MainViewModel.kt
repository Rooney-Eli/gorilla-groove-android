package com.example.ggmobileredux.ui.library

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.retrofit.LoginRequest
import com.example.ggmobileredux.service.EMPTY_PLAYBACK_STATE
import com.example.ggmobileredux.service.MusicServiceConnection
import com.example.ggmobileredux.util.Constants.KEY_TRACKS
import com.example.ggmobileredux.util.DataState
import com.example.ggmobileredux.util.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


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


    private val _currentTrackItem: MutableLiveData<MediaMetadataCompat> = MutableLiveData()
    val currentTrackItem: LiveData<MediaMetadataCompat>
        get() = _currentTrackItem

    private val _playPauseState: MutableLiveData<PlaybackStateCompat> = MutableLiveData()
    val playPauseState: LiveData<PlaybackStateCompat>
        get() = _playPauseState


    private val _dataState: MutableLiveData<DataState<*>> = MutableLiveData()
    val dataState: LiveData<DataState<*>>
        get() = _dataState

    private val _sessionState: MutableLiveData<SessionState<*>> = MutableLiveData()
    val sessionState: LiveData<SessionState<*>>
        get() = _sessionState

    val mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }

    @ExperimentalCoroutinesApi
    fun setStateEvent(mainStateEvent: MainStateEvent<Int>) {
        viewModelScope.launch {
            when (mainStateEvent) {
                is MainStateEvent.GetAllTracksEvents -> {
                    mainRepository.getAllTracks()
                        .onEach {
                            _dataState.value = it
                        }
                        .launchIn(viewModelScope)
                }
                is MainStateEvent.GetTrackEvent<Int> -> {
                    mainRepository.getTrackWithLink(mainStateEvent.data)
                        .onEach {
                            _dataState.value = it
                        }
                        .launchIn(viewModelScope)
                }

                is MainStateEvent.GetListOfTracksEvent<*> -> {
                    mainRepository.getListOfTracksWithLinks(mainStateEvent.data as List<Int>)
                        .onEach {
                            _dataState.value = it
                        }
                        .launchIn(viewModelScope)
            }

                is MainStateEvent.None -> {
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
                            _sessionState.value = it
                        }
                        .launchIn(viewModelScope)
                }
                is LoginStateEvent.None -> {
                    //ignored
                }
            }
        }
    }

    fun playMediaId(mediaId: String) {
        val transportControls = musicServiceConnection.transportControls
        transportControls.playFromMediaId(mediaId, null)
    }

    fun playMediaList(mediaList: List<Track>) {
        val transportControls = musicServiceConnection.transportControls
        val stringArray = mediaList.map { it.id.toString() }.toTypedArray()

        //TODO create a constant if this works
        val bundle = Bundle()
        bundle.putStringArray(KEY_TRACKS, stringArray)

        transportControls.playFromMediaId(mediaList[0].id.toString(), bundle)
    }

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE
        _playPauseState.postValue(playbackState)
    }

    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        _currentTrackItem.postValue(it)
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

sealed class MainStateEvent<out R> {

    object GetAllTracksEvents: MainStateEvent<Nothing>()
    data class GetTrackEvent<out T>(val data: T): MainStateEvent<T>()
    data class GetListOfTracksEvent<out T>(val data: Collection<T>): MainStateEvent<T>()
    object None: MainStateEvent<Nothing>()
}

