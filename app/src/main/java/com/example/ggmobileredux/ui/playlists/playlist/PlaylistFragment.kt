package com.example.ggmobileredux.ui.playlists.playlist

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Playlist
import com.example.ggmobileredux.model.PlaylistKey
import com.example.ggmobileredux.ui.MainViewModel
import com.example.ggmobileredux.ui.PlayerControlsViewModel
import com.example.ggmobileredux.ui.PlaylistsEvent
import com.example.ggmobileredux.ui.playlists.PlaylistKeyAdapter
import com.example.ggmobileredux.util.Constants
import com.example.ggmobileredux.util.Constants.CALLING_FRAGMENT_PLAYLIST
import com.example.ggmobileredux.util.StateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_playlist.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlaylistFragment : Fragment(R.layout.fragment_playlist), PlaylistAdapter.OnPlaylistListener {

    val TAG = "AppDebug"
    private val viewModel: MainViewModel by viewModels()
    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()
    lateinit var playlistAdapter: PlaylistAdapter
    var playlistKeyId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistKeyId = arguments?.getInt("PLAYLIST_KEY_ID")
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        subscribeObservers()

        //val keyId = savedInstanceState?.getInt("PLAYLIST_KEY_ID")
        playlistKeyId?.let {
            viewModel.setPlaylistsEvent(PlaylistsEvent.GetPlaylist(it))
        }

    }

    private fun setupRecyclerView() = playlist_rv.apply {
        playlistAdapter = PlaylistAdapter(this@PlaylistFragment)
        adapter = playlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.playlist.observe(requireActivity(), Observer {
            Log.d(TAG, "subscribeObservers: $it")
            when (it.stateEvent) {
                is StateEvent.Success -> {
                    playlistAdapter.submitPlaylist(it.data as Playlist)
                }
                is StateEvent.Error -> {
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                }
                is StateEvent.Loading -> {
                }
            }
        })
    }
    override fun onPlaylistClick(position: Int) {
        Log.d(TAG, "onPlaylistClick: Clicked ${playlistAdapter.playlistItems[position].track}")
        playerControlsViewModel.playMedia(
            playlistAdapter.playlistItems[position].track,
            CALLING_FRAGMENT_PLAYLIST,
            playlistKeyId
        )
        //viewModel.setNowPlayingTracks(playlistAdapter.playlistItems.map { playlistItem -> playlistItem.track.id })

    }

    override fun onPlaylistLongClick(position: Int): Boolean {
        Log.d(TAG, "onPlaylistLongClick: Long Clicked ${playlistAdapter.playlistItems[position].track}")
        return true
    }
}