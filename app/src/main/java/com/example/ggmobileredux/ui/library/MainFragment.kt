package com.example.ggmobileredux.ui.library

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuItemCompat.getActionView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Track
import com.example.ggmobileredux.retrofit.TrackLinkResponse
import com.example.ggmobileredux.service.MusicService
import com.example.ggmobileredux.ui.CoolAdapter
import com.example.ggmobileredux.util.Constants
import com.example.ggmobileredux.util.Constants.KEY_SORT
import com.example.ggmobileredux.util.Constants.SORT_BY_AZ
import com.example.ggmobileredux.util.Constants.SORT_BY_DATE_ADDED_NEWEST
import com.example.ggmobileredux.util.Constants.SORT_BY_DATE_ADDED_OLDEST
import com.example.ggmobileredux.util.Constants.SORT_BY_ID
import com.example.ggmobileredux.util.StateEvent
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main),  PlaylistAdapter.OnTrackListener {
    val TAG = "AppDebug"
    private val viewModel: MainViewModel by viewModels()
    lateinit var playlistAdapter: PlaylistAdapter
    lateinit var listOfTrack: List<Track>

    @Inject
    lateinit var sharedPref: SharedPreferences

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)



        Log.d(TAG, "onViewCreated: Retrieving tracks for recycler view from viewmodel...")
        viewModel.setStateEvent(MainStateEvent.GetAllTracksEvents)
        setupRecyclerView()
        subscribeObservers()
    }

    private fun setupRecyclerView() = playlist_rv.apply {
        playlistAdapter = PlaylistAdapter(this@MainFragment)
        adapter = playlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(requireActivity(), Observer {
            when (it.stateEvent) {
                is StateEvent.Success -> {
                    displayProgressBar(false)
                    listOfTrack = it.data as List<Track>
                    playlistAdapter.submitList(it.data as List<Track>)
                    playlistAdapter.notifyDataSetChanged()
                }
                is StateEvent.TrackSuccess -> {
                    displayProgressBar(false)
                    val track = it.data as Track
                    viewModel.playMediaId(track.id.toString())
                }
                is StateEvent.Error -> {
                    displayProgressBar(false)
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                }
                is StateEvent.Loading -> {
                    displayProgressBar(true)
                }
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE;

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                playlistAdapter.filter.filter(newText);
                return false;
            }
        })

    }

    @ExperimentalCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_sort_az -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_AZ)
                    .apply()
                viewModel.setStateEvent(MainStateEvent.GetAllTracksEvents)
                true
            }
            R.id.action_sort_id -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_ID)
                    .apply()
                viewModel.setStateEvent(MainStateEvent.GetAllTracksEvents)
                true
            }
            R.id.action_sort_date_added_oldest -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_DATE_ADDED_OLDEST)
                    .apply()
                viewModel.setStateEvent(MainStateEvent.GetAllTracksEvents)
                true
            }
            R.id.action_sort_date_added_newest -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_DATE_ADDED_NEWEST)
                    .apply()
                viewModel.setStateEvent(MainStateEvent.GetAllTracksEvents)
                true
            }

            R.id.action_settings -> {
                Toast.makeText(requireActivity(), "Settings?", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayProgressBar(isDisplayed: Boolean) {
        if(isDisplayed){
            progress_bar.visibility = View.VISIBLE
            progress_bar.bringToFront()
        } else {
            progress_bar.visibility =  View.GONE
        }
    }

    @ExperimentalCoroutinesApi
    override fun onTrackClick(position: Int) {
        Log.d(TAG, "onTrackClick: $position")
        viewModel.setStateEvent(
            MainStateEvent.GetTrackEvent(
                playlistAdapter.filteredList[position].id
            )
        )
    }

    private fun writePersonalDataToSharedPref() {

        sharedPref.edit()
            .putString(KEY_SORT, SORT_BY_AZ)
            .apply()

    }

}