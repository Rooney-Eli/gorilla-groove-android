package com.example.ggmobileredux.ui.playlists


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Playlist
import com.example.ggmobileredux.model.PlaylistItem
import com.example.ggmobileredux.model.PlaylistKey
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.ui.MainViewModel
import com.example.ggmobileredux.ui.PlaylistsEvent
import com.example.ggmobileredux.util.StateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_playlists.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_playlists), PlaylistKeyAdapter.OnPlaylistListener {
    val TAG = "AppDebug: PlaylistFragment: "

    private val viewModel: MainViewModel by viewModels()
    lateinit var playlistKeyAdapter: PlaylistKeyAdapter
    private var savedInstanceStateBundle: Bundle? = null

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceStateBundle = savedInstanceState
        setupRecyclerView()
        subscribeObservers()
        viewModel.setPlaylistsEvent(PlaylistsEvent.GetAllPlaylistKeys)
    }

    private fun setupRecyclerView() = playlists_key_rv.apply {
        playlistKeyAdapter = PlaylistKeyAdapter(this@PlaylistsFragment)
        adapter = playlistKeyAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.playlistKeys.observe(requireActivity(), Observer {
            when (it.stateEvent) {
                is StateEvent.Success -> {
                        playlistKeyAdapter.submitPlaylistMap((it.data as List<PlaylistKey>))
                }
                is StateEvent.Error -> {
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                }
                is StateEvent.Loading -> {
                }
            }
        })
    }

    @ExperimentalCoroutinesApi
    override fun onPlaylistClick(position: Int) {
        Log.d(TAG, "onPlaylistClick: Clicked: $position")
        val playlistKeyId = playlistKeyAdapter.playlistKeyList[position].id
        val bundle = bundleOf("PLAYLIST_KEY_ID" to playlistKeyId)

        findNavController().navigate(
            R.id.action_playlistsFragment_to_playlistFragment,
            bundle
        )
    }

    override fun onPlaylistLongClick(position: Int): Boolean {
        Log.d(TAG, "onPlaylistLongClick: Long Clicked: $position")

        return true
    }
}