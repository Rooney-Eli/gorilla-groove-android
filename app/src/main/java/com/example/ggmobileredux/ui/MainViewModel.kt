package com.example.ggmobileredux.ui

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
//
//    private val _playlistMap: MutableLiveData<DataState<out Map<PlaylistKey, Playlist>>> = MutableLiveData()
//    val playlistMap: LiveData<DataState<out Map<PlaylistKey, Playlist>>>
//        get() = _playlistMap

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
                is LoginStateEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setLibraryEvent(libraryEvent: LibraryEvent<Int>) {
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
    fun setPlaylistsEvent(playlistsEvent: PlaylistsEvent<Int>) {
        viewModelScope.launch {
            when (playlistsEvent) {
                is PlaylistsEvent.GetAllPlaylistKeys -> {
                    mainRepository.getAllPlaylistKeys()
                        .onEach {
                            _playlistKeys.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }
                is PlaylistsEvent.GetPlaylist<Int> -> {
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

    fun getNowPlayingTracks() {
        _nowPlayingTracks.postValue(mainRepository.fetchNowPlayingTracks())
    }

    fun setNowPlayingTracks(trackIds: List<Int> ) {
        mainRepository.setNowPlayingTracks(trackIds)
        _nowPlayingTracks.postValue(mainRepository.fetchNowPlayingTracks())
            //.also { playMedia(it[0], CALLING_FRAGMENT_NOW_PLAYING) }
    }

    fun sortTracks(sort: Sort) {
        mainRepository.sortTracks(sort)
        _libraryTracks.postValue(DataState(mainRepository.sortedTrackList, StateEvent.Success))
    }

}

sealed class LoginStateEvent<out R> {
    data class LoginEvent<out T>(val data: T): LoginStateEvent<T>()
    object None: LoginStateEvent<Nothing>()
}

sealed class LibraryEvent<out R> {
    object GetAllTracksEvents: LibraryEvent<Nothing>()
    object None: LibraryEvent<Nothing>()
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