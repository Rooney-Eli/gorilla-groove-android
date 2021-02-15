package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.gorilla.gorillagroove.model.Track
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.EMPTY_PLAYBACK_STATE
import com.gorilla.gorillagroove.service.MusicServiceConnection
import com.gorilla.gorillagroove.util.Constants


class PlayerControlsViewModel @ViewModelInject constructor(
    private val repository: MainRepository,
    musicServiceConnection: MusicServiceConnection,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = "AppDebug: PlayerControlsViewModel: "

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


    private var isPlaying = false
    private var currentMetadata : MediaMetadataCompat? = null
    private fun sendPlayStatusToServer() {
        if(isPlaying && currentMetadata != null) {
            currentMetadata?.description?.let { currTrack ->
//                //Log.d(TAG, "Sending Now Playing to server: ${currTrack.title}: ")
                repository.sendNowPlayingToServer(
                    currTrack
                )
            }
        } else if(!isPlaying && currentMetadata != null) {
            repository.sendStoppedPlayingToServer()
        } else {
            //probably initialization
        }

    }

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        _playbackState.postValue(it ?: EMPTY_PLAYBACK_STATE)

//        when(it.state) {
//            PlaybackStateCompat.STATE_PLAYING -> ////Log.d(TAG, "STATE: PLAYING")
//            PlaybackStateCompat.STATE_PAUSED -> ////Log.d(TAG, "STATE: PAUSED")
//            PlaybackStateCompat.STATE_STOPPED -> ////Log.d(TAG, "STATE: STOPPED")
//            PlaybackStateCompat.STATE_BUFFERING -> ////Log.d(TAG, "STATE: BUFFERING")
//        }

        when{
            it.isPlaying -> {
                isPlaying = true
                sendPlayStatusToServer()
            }

            it.isPaused -> {
                isPlaying = false
                sendPlayStatusToServer()
            }
        }
    }

    private val repeatStateObserver = Observer<Int> {
        _repeatState.postValue(it)
    }
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        _currentTrackItem.postValue(it)
        if(it?.description?.mediaId != "") {
            if(currentMetadata?.description?.mediaId != it.description?.mediaId) {
                currentMetadata = it
                sendPlayStatusToServer()
                it.description.mediaId?.let { it1 -> Integer.parseInt(it1).toLong() }?.let {
                    val trackInNP = repository.nowPlayingTracks.find { track -> it == track.id }
                    val index = repository.nowPlayingTracks.indexOf(trackInNP)
                    repository.currentIndex = index

//                    ////Log.d(TAG, "current index: ${repository.currentIndex}")
                }

            }
        }


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

    fun playMedia(track: Track, callingFragment: String, playlistId: Long?) {

        repository.changeMediaSource(callingFragment, playlistId)

        val extras = Bundle().also { it.putString(Constants.KEY_CALLING_FRAGMENT, callingFragment) }
        transportControls.playFromMediaId(track.id.toString(), extras)
    }

    fun playNow(track: Track, callingFragment: String, playlistId: Int?) {
        val extras = Bundle().also { it.putString(Constants.KEY_CALLING_FRAGMENT, callingFragment) }
        transportControls.playFromMediaId(track.id.toString(), extras)
    }

    fun playPause(): Boolean {
        return if(_playbackState.value?.isPaused == true) {
            transportControls.play()
            true
        } else {
            transportControls.pause()
            false
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

    fun logout() {
        musicServiceConnection.transportControls.stop()
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
