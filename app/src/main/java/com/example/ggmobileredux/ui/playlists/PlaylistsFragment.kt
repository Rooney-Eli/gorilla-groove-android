package com.example.ggmobileredux.ui.playlists


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.model.Playlist
import com.example.ggmobileredux.ui.library.MainViewModel
import com.example.ggmobileredux.ui.library.PlaylistsEvent
import com.example.ggmobileredux.util.StateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_playlists.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_playlists), PlaylistAdapter.OnPlaylistListener {

    val TAG = "AppDebug"
    private val viewModel: MainViewModel by viewModels()
    lateinit var playlistAdapter: PlaylistAdapter

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        subscribeObservers()
        viewModel.setPlaylistsEvent(PlaylistsEvent.GetAllPlaylists)
    }

    private fun setupRecyclerView() = playlists_rv.apply {
        playlistAdapter = PlaylistAdapter(this@PlaylistsFragment)
        adapter = playlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.playlists.observe(requireActivity(), Observer {
            when (it.stateEvent) {
                is StateEvent.Success -> {
//                    displayProgressBar(false)
                    playlistAdapter.submitList(it.data as List<Playlist>)
                }
                is StateEvent.Error -> {
//                    displayProgressBar(false)
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                }
                is StateEvent.Loading -> {
//                    displayProgressBar(true)
                }
            }
        })
    }

    override fun onPlaylistClick(position: Int) {
        Log.d(TAG, "onPlaylistClick: clicked ${playlistAdapter.playlistList[position].name}")
    }

    override fun onPlaylistLongClick(position: Int): Boolean {
        Log.d(TAG, "onPlaylistLongClick: long clicked ${playlistAdapter.playlistList[position].name}")
        return true
    }


}