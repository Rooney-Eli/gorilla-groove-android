package com.example.ggmobileredux.ui.playing

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.ui.library.MainViewModel
import com.example.ggmobileredux.ui.library.PlaylistAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_playing.*

@AndroidEntryPoint
class PlayingFragment : Fragment(R.layout.fragment_playing), PlaylistAdapter.OnTrackListener {
    private val TAG = "AppDebug: Now Playing: "

    private val viewModel: MainViewModel by viewModels()

    lateinit var trackListAdapter: PlaylistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        subscribeObservers()
        viewModel.getNowPlayingTracks()
    }


    private fun setupRecyclerView() = tracklist_rv.apply {
        trackListAdapter = PlaylistAdapter(this@PlayingFragment)
        adapter = trackListAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.nowPlayingTracks.observe(requireActivity(), Observer {
            trackListAdapter.submitList(it)
        })
    }


    override fun onTrackClick(position: Int) {
        Log.d(TAG, "onTrackClick: ${trackListAdapter.trackList[position]}")
        viewModel.playMedia(trackListAdapter.trackList[position])
    }

    override fun onTrackLongClick(position: Int): Boolean {
        Log.d(TAG, "onTrackLongClick: ")
        return true
    }

}