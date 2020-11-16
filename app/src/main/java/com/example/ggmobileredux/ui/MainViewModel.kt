package com.example.ggmobileredux.ui

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.ggmobileredux.model.Playlist
import com.example.ggmobileredux.model.PlaylistKey
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.model.User
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.repository.Sort
import com.example.ggmobileredux.network.login.LoginRequest
import com.example.ggmobileredux.repository.SelectionOperation
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
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "AppDebug"

    private val _loginState: MutableLiveData<SessionState<*>> = MutableLiveData()
    val loginState: LiveData<SessionState<*>>
        get() = _loginState

    private val _libraryTracks: MutableLiveData<DataState<out List<Track>>> = MutableLiveData()
    val libraryTracks: LiveData<DataState<out List<Track>>>
        get() = _libraryTracks

    private val _nowPlayingTracks: MutableLiveData<List<Track>> = MutableLiveData()
    val nowPlayingTracks: LiveData<List<Track>>
        get() = _nowPlayingTracks

    private val _users: MutableLiveData<DataState<out List<User>>> = MutableLiveData()
    val users: LiveData<DataState<out List<User>>>
        get() = _users

    private val _playlistKeys: MutableLiveData<DataState<out List<PlaylistKey>>> = MutableLiveData()
    val playlistKeys: LiveData<DataState<out List<PlaylistKey>>>
        get() = _playlistKeys

    private val _playlist: MutableLiveData<DataState<out Playlist>> = MutableLiveData()
    val playlist: LiveData<DataState<out Playlist>>
        get() = _playlist

//    fun getNowPlayingTracks() {
//        _nowPlayingTracks.postValue(mainRepository.playingTracks)
//    }

    @ExperimentalCoroutinesApi
    fun setLoginStateEvent(loginStateEvent: LoginStateEvent<LoginRequest>) {
        viewModelScope.launch {
            when (loginStateEvent) {
                is LoginStateEvent.LoginEvent<LoginRequest> -> {
                    mainRepository.getToken(loginStateEvent.data)
                        .onEach {
                            _loginState.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }
                is LoginStateEvent.LogoutEvent -> {
                    mainRepository.logoutUser()
                }
                is LoginStateEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setNowPlayingEvent(nowPlayingEvent: NowPlayingEvent<Nothing>) {
        viewModelScope.launch {
            when (nowPlayingEvent) {
                is NowPlayingEvent.GetNowPlayingTracksEvent -> {
                    mainRepository.getNowPlayingTracks()
                        .onEach {
                            _nowPlayingTracks.postValue(it.data)
                            Log.d(TAG, "setNowPlayingEvent: updated track listing!")
                        }
                        .launchIn(viewModelScope)
                }
                is NowPlayingEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setLibraryEvent(libraryEvent: LibraryEvent<Long>) {
        viewModelScope.launch {
            when (libraryEvent) {
                is LibraryEvent.GetAllTracksEvents -> {
                    mainRepository.getAllTracks()
                        .onEach {
                            _libraryTracks.postValue(it)
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
    fun setPlaylistsEvent(playlistsEvent: PlaylistsEvent<Long>) {
        viewModelScope.launch {
            when (playlistsEvent) {
                is PlaylistsEvent.GetAllPlaylistKeys -> {
                    mainRepository.getAllPlaylistKeys()
                        .onEach {
                            _playlistKeys.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }
                is PlaylistsEvent.GetPlaylist<Long> -> {
                    mainRepository.getPlaylist(playlistsEvent.data)
                        .onEach {
                            _playlist.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }
                is PlaylistsEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setUsersEvent(usersEvent: UsersEvent<Nothing>) {
        viewModelScope.launch {
            when (usersEvent) {
                is UsersEvent.GetAllUsers -> {
                    mainRepository.getAllUsers()
                        .onEach {
                            _users.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }
                is UsersEvent.None -> {
                    //ignored
                }
            }
        }
    }

    fun sortTracks(sorting: Sort) {
        mainRepository.sortLibrary(sorting)
        _libraryTracks.postValue(DataState(mainRepository.allLibraryTracks, StateEvent.Success))
    }

    @ExperimentalCoroutinesApi
    fun setSelectedTracks(trackIds: List<Long>, selectionOperation: SelectionOperation) {
        mainRepository.setSelectedTracks(trackIds, selectionOperation)
        Log.d(TAG, "setSelectedTracks: setting new tracks")
        setNowPlayingEvent(NowPlayingEvent.GetNowPlayingTracksEvent)
    }

}

sealed class LoginStateEvent<out R> {
    data class LoginEvent<out T>(val data: T): LoginStateEvent<T>()
    object LogoutEvent : LoginStateEvent<Nothing>()
    object None: LoginStateEvent<Nothing>()
}

sealed class LibraryEvent<out R> {
    object GetAllTracksEvents: LibraryEvent<Nothing>()
    object None: LibraryEvent<Nothing>()
}

sealed class NowPlayingEvent<out R> {
    object GetNowPlayingTracksEvent: NowPlayingEvent<Nothing>()
    object None: NowPlayingEvent<Nothing>()
}

sealed class UsersEvent<Nothing> {
    object GetAllUsers: UsersEvent<Nothing>()
    object None: UsersEvent<Nothing>()
}

sealed class PlaylistsEvent<out R> {
    object GetAllPlaylistKeys: PlaylistsEvent<Nothing>()
    data class GetPlaylist<out T>(val data: T): PlaylistsEvent<T>()
    object None: PlaylistsEvent<Nothing>()
}