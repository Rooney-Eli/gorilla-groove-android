package com.example.ggmobileredux.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.service.EMPTY_PLAYBACK_STATE
import com.example.ggmobileredux.service.MusicServiceConnection
import com.example.ggmobileredux.util.Constants


class PlayerControlsViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    musicServiceConnection: MusicServiceConnection,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = "AppDebug"

    private val transportControls: MediaControllerCompat.TransportControls by lazy { musicServiceConnection.transportControls }

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())


    //Controls and Player Data
    private val _currentTrackItem: MutableLiveData<MediaMetadataCompat> = MutableLiveData()
    val currentTrackItem: LiveData<MediaMetadataCompat>
        get() = _currentTrackItem

    private val _playbackState: MutableLiveData<PlaybackStateCompat> = MutableLiveData()
    val playbackState: LiveData<PlaybackStateCompat>
        get() = _playbackState

    private val _repeatState: MutableLiveData<Int> = MutableLiveData()
    val repeatState: LiveData<Int>
        get() = _repeatState

    private val _isBuffering: MutableLiveData<Boolean> = MutableLiveData()
    val isBuffering: LiveData<Boolean>
        get() = _isBuffering


    val mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }

    val bufferPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        _playbackState.postValue(it ?: EMPTY_PLAYBACK_STATE)

        when(it.state) {
            PlaybackStateCompat.STATE_PLAYING -> Log.d(TAG, "STATE: PLAYING")
            PlaybackStateCompat.STATE_PAUSED -> Log.d(TAG, "STATE: PAUSED")
            PlaybackStateCompat.STATE_STOPPED -> Log.d(TAG, "STATE: STOPPED")
            PlaybackStateCompat.STATE_BUFFERING -> Log.d(TAG, "STATE: BUFFERING")
        }

        when{
            it.isPlaying -> {
                _currentTrackItem.value?.description?.let { currTrack ->
                    repository.sendNowPlayingToServer(
                        currTrack
                    )
                }
            }

            it.isPaused -> {
                _currentTrackItem.value?.description?.let { currTrack ->
                    repository.sendStoppedPlayingToServer(
                        currTrack
                    )
                }
            }
        }
    }

    private val repeatStateObserver = Observer<Int> {
        _repeatState.postValue(it)
    }
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        _currentTrackItem.postValue(it)
    }

    private val musicServiceConnection = musicServiceConnection.also {

        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
        it.repeatState.observeForever(repeatStateObserver)
        checkPlaybackPosition()
    }

    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        if (mediaPosition.value != _playbackState.value?.currentPlayBackPosition) {
            mediaPosition.postValue(_playbackState.value?.currentPlayBackPosition)
        }
        if (updatePosition)
            checkPlaybackPosition()
    }, POSITION_UPDATE_INTERVAL_MILLIS)

    fun playMedia(track: Track, callingFragment: String) {
        val extras = Bundle().also { it.putString(Constants.KEY_CALLING_FRAGMENT, callingFragment) }
        transportControls.playFromMediaId(track.id.toString(), extras)
    }

    fun playPause() {
        if(_playbackState.value?.isPaused == true) {
            transportControls.play()
        } else {
            transportControls.pause()
        }
    }

    fun seekTo(position: Long) {
        transportControls.seekTo(position)
    }

    fun skipToNext() {
        transportControls.skipToNext()
    }

    fun skipToPrevious() {
        transportControls.skipToPrevious()
    }


    fun repeat() {
        when(_repeatState.value) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> {
                transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
            }
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            }
            else -> {
                transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)
        musicServiceConnection.repeatState.removeObserver(repeatStateObserver)
        updatePosition = false
    }

}

private const val POSITION_UPDATE_INTERVAL_MILLIS = 1000L

inline val PlaybackStateCompat.isPaused
    get() = (state == PlaybackStateCompat.STATE_PAUSED) ||
            state == PlaybackStateCompat.STATE_STOPPED

inline val PlaybackStateCompat.isPlaying
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING)

inline val PlaybackStateCompat.currentPlayBackPosition: Long
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
                val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
                (position + (timeDelta * playbackSpeed)).toLong()
            } else {
                position
            }
