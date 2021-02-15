package com.gorilla.gorillagroove.ui.playing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.ui.PlayerControlsViewModel
import com.gorilla.gorillagroove.ui.MainViewModel
import com.gorilla.gorillagroove.ui.NowPlayingEvent
import com.gorilla.gorillagroove.ui.isPlaying
import com.gorilla.gorillagroove.ui.library.PlaylistAdapter
import com.gorilla.gorillagroove.util.Constants.CALLING_FRAGMENT_NOW_PLAYING
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_playing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class PlayingFragment : Fragment(R.layout.fragment_playing), PlaylistAdapter.OnTrackListener {
    private val TAG = "AppDebug: Now Playing: "

    private val viewModel: MainViewModel by viewModels()
    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()

    private lateinit var trackListAdapter: PlaylistAdapter

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        subscribeObservers()
        viewModel.setNowPlayingEvent(NowPlayingEvent.GetNowPlayingTracksEvent)
    }


    private fun setupRecyclerView() = tracklist_rv.apply {
        trackListAdapter = PlaylistAdapter(this@PlayingFragment)
        adapter = trackListAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.nowPlayingTracks.observe(requireActivity(), Observer {
            trackListAdapter.submitList(it)
            trackListAdapter.notifyDataSetChanged()
            //Log.d(TAG, "subscribeObservers: submitted new items!")
        })

        playerControlsViewModel.currentTrackItem.observe(requireActivity(), Observer {

            val mediaId = it.description.mediaId.toString()
            if(mediaId != "") {
                trackListAdapter.playingTrackId = mediaId
                trackListAdapter.notifyDataSetChanged()
            }
        })

        playerControlsViewModel.playbackState.observe(requireActivity(), Observer {
            trackListAdapter.isPlaying = it.isPlaying
            trackListAdapter.notifyDataSetChanged()

        })
    }

    override fun onTrackClick(position: Int) {
        //Log.d(TAG, "onTrackClick: ${trackListAdapter.trackList[position]}")
        playerControlsViewModel.playMedia(trackListAdapter.trackList[position], CALLING_FRAGMENT_NOW_PLAYING, null)
    }

    override fun onTrackLongClick(position: Int): Boolean {
        //Log.d(TAG, "onTrackLongClick: ")
        return true
    }

    override fun onPlayPauseClick(position: Int) {
        playerControlsViewModel.playPause()
    }

    override fun onOptionsClick(position: Int) {
        //Log.d(TAG, "onOptionsClick: ")
    }

    override fun onPlayNextSelection(position: Int) {
        //Log.d(TAG, "onPlayNextSelection: ")
    }

    override fun onPlayLastSelection(position: Int) {
        //Log.d(TAG, "onPlayLastSelection: ")
    }

    override fun onGetLinkSelection(position: Int) {
        //Log.d(TAG, "onGetLinkSelection: ")
    }

    override fun onDownloadSelection(position: Int) {
        //Log.d(TAG, "onDownloadSelection: ")
    }

    override fun onRecommendSelection(position: Int) {
        //Log.d(TAG, "onRecommendSelection: ")
    }

    override fun onAddToPlaylistSelection(position: Int) {
        //Log.d(TAG, "onAddToPlaylistSelection: ")
    }
    override fun onPropertiesSelection(position: Int) {
        //Log.d(TAG, "onPropertiesSelection: ")
    }

}