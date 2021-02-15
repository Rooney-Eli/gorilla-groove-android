package com.gorilla.gorillagroove.ui.library

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.model.Track
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.repository.Sort
import com.gorilla.gorillagroove.ui.LibraryEvent
import com.gorilla.gorillagroove.ui.MainViewModel
import com.gorilla.gorillagroove.ui.PlayerControlsViewModel
import com.gorilla.gorillagroove.ui.isPlaying
import com.gorilla.gorillagroove.util.Constants.CALLING_FRAGMENT_LIBRARY
import com.gorilla.gorillagroove.util.Constants.KEY_SORT
import com.gorilla.gorillagroove.util.Constants.SORT_BY_ARTIST_AZ
import com.gorilla.gorillagroove.util.Constants.SORT_BY_AZ
import com.gorilla.gorillagroove.util.Constants.SORT_BY_DATE_ADDED_NEWEST
import com.gorilla.gorillagroove.util.Constants.SORT_BY_DATE_ADDED_OLDEST
import com.gorilla.gorillagroove.util.Constants.SORT_BY_ID
import com.gorilla.gorillagroove.util.StateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject


@AndroidEntryPoint
class LibraryFragment : Fragment(R.layout.fragment_main),  PlaylistAdapter.OnTrackListener {
    val TAG = "AppDebug"
    private val viewModel: MainViewModel by viewModels()
    private val playerControlsViewModel: PlayerControlsViewModel by viewModels()
    lateinit var playlistAdapter: PlaylistAdapter
    var actionMode : ActionMode? = null


    @Inject
    lateinit var sharedPref: SharedPreferences

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.setLibraryEvent(LibraryEvent.GetAllTracksEvents)
        setupRecyclerView()
        subscribeObservers()
    }

    private fun setupRecyclerView() = playlist_rv.apply {
        playlistAdapter = PlaylistAdapter(this@LibraryFragment)
        adapter = playlistAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.libraryTracks.observe(requireActivity(), Observer {
            when (it.stateEvent) {
                is StateEvent.Success -> {
                    displayProgressBar(false)
                    playlistAdapter.submitList(it.data as List<Track>)
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
        playerControlsViewModel.currentTrackItem.observe(requireActivity(), Observer {
            val mediaId = it.description.mediaId.toString()
            if(mediaId != "") {
                playlistAdapter.playingTrackId = mediaId
                playlistAdapter.notifyDataSetChanged()
            }
        })
        playerControlsViewModel.playbackState.observe(requireActivity(), Observer {
            playlistAdapter.isPlaying = it.isPlaying
            playlistAdapter.notifyDataSetChanged()

        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                playlistAdapter.filter.filter(newText)
                return false
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
                viewModel.sortTracks(Sort.A_TO_Z)
                true
            }
            R.id.action_sort_id -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_ID)
                    .apply()
                viewModel.sortTracks(Sort.ID)
                true
            }
            R.id.action_sort_date_added_oldest -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_DATE_ADDED_OLDEST)
                    .apply()
                viewModel.sortTracks(Sort.OLDEST)
                true
            }
            R.id.action_sort_date_added_newest -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_DATE_ADDED_NEWEST)
                    .apply()
                viewModel.sortTracks(Sort.NEWEST)
                true
            }
            R.id.action_sort_artist_az -> {
                sharedPref.edit()
                    .putString(KEY_SORT, SORT_BY_ARTIST_AZ)
                    .apply()
                viewModel.sortTracks(Sort.ARTIST_A_TO_Z)
                true
            }

            R.id.action_update_tracks -> {
                viewModel.setLibraryEvent(LibraryEvent.UpdateAllTracks)
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


    override fun onTrackClick(position: Int) {
        val clickedTrack = playlistAdapter.filteredList[position]
        //Log.d(TAG, "onTrackClick: $clickedTrack")
        playerControlsViewModel.playMedia(clickedTrack, CALLING_FRAGMENT_LIBRARY, null)
    }

    override fun onTrackLongClick(position: Int): Boolean {
        //Log.d(TAG, "onTrackLongClick: Long clicked $position")
        return when (actionMode) {
            null -> {
                playlistAdapter.showingCheckBox = true
                playlistAdapter.notifyDataSetChanged()
                actionMode = activity?.startActionMode(actionModeCallback)!!
                view?.isSelected = true
                true
            }
            else -> {

                false
            }
        }
    }

    override fun onPlayPauseClick(position: Int) {
        playlistAdapter.isPlaying = playerControlsViewModel.playPause()
        playlistAdapter.notifyDataSetChanged()
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

        val track = playlistAdapter.filteredList[position]
        val bundle = bundleOf("KEY_TRACK_ID" to track.id)

        findNavController().navigate(
            R.id.action_mainFragment_to_trackPropertiesFragment,
            bundle
        )
    }



    @ExperimentalCoroutinesApi
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
            val inflater: MenuInflater? = mode?.menuInflater
            mode?.title = "Selecting tracks..."
            inflater?.inflate(R.menu.context_action_menu, menu)

            //required because the XML is overridden
            menu[0].setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            menu[1].setShowAsAction(SHOW_AS_ACTION_NEVER)
            menu[2].setShowAsAction(SHOW_AS_ACTION_NEVER)
            menu[3].setShowAsAction(SHOW_AS_ACTION_NEVER)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_play_now_button -> {
                    val selectedTracks = playlistAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NOW)
                    playlistAdapter.trackList.find { track -> track.id == selectedTracks[0] }?.let {
                        playerControlsViewModel.playNow(it, CALLING_FRAGMENT_LIBRARY, null)
                    }
                    mode?.finish()
                    true
                }
                R.id.action_play_now -> {
                    val selectedTracks = playlistAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NOW)
                    playlistAdapter.trackList.find { track -> track.id == selectedTracks[0] }?.let {
                        playerControlsViewModel.playNow(it, CALLING_FRAGMENT_LIBRARY, null)
                    }
                    mode?.finish()
                    true
                }
                R.id.action_play_next -> {
                    val selectedTracks = playlistAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_NEXT)
                    mode?.finish()
                    true
                }
                R.id.action_play_last -> {
                    val selectedTracks = playlistAdapter.getSelectedTracks()
                    viewModel.setSelectedTracks(selectedTracks, SelectionOperation.PLAY_LAST)
                    mode?.finish()
                    true
                }



                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            playlistAdapter.showingCheckBox = false
            playlistAdapter.checkedTracks.clear()
            playlistAdapter.notifyDataSetChanged()
            actionMode = null
        }
    }



}


