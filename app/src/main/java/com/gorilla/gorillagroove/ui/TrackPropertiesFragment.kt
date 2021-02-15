package com.gorilla.gorillagroove.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.model.Track
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.util.StateEvent
import com.gorilla.gorillagroove.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_track_properties.*
import kotlinx.coroutines.ExperimentalCoroutinesApi


@AndroidEntryPoint
class TrackPropertiesFragment : Fragment(R.layout.fragment_track_properties) {
    private val viewModel: MainViewModel by viewModels()

    var trackId: Long? = null
    var track: Track? = null

    //differs
    var newName: String? = null
    var newArtist: String? = null
    var newFeaturing: String? = null
    var newAlbum: String? = null
    var newGenre: String? = null
    var newTrackNum: Int? = null
    var newYear: Int? = null
    var newNote: String? = null

    var hasChanged: Boolean = false

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trackId = arguments?.getLong("KEY_TRACK_ID")


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }



    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeObservers()

        trackId?.let {
            viewModel.setLibraryEvent(LibraryEvent.GetTrack(it))
        }
    }

    

    override fun onPause() {
        hideKeyboard(requireActivity())
        super.onPause()
    }


    private fun subscribeObservers() {
        viewModel.selectedTrack.observe(requireActivity(), Observer {
            when (it.stateEvent) {
                is StateEvent.Success -> {
                    it.data?.let { it1 -> 
                        track = it1
                        populateFragmentText(it1)                    
                    }
                    listenForChanges()
                    top_level_layout.requestFocus()
                }
                is StateEvent.Error -> {
                    Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show()
                }
                is StateEvent.Loading -> {
                    //nothing
                }
            }
        })

    }

    private fun populateFragmentText(track: Track) {
            et_name.setText(track.name)
            et_artist.setText(track.artist)
            et_featuring.setText(track.featuring)
            et_album.setText(track.album)
            et_genre.setText(track.genre ?: "")
            et_track_number.setText(track.trackNumber?.toString() ?: "")
            et_year.setText(track.releaseYear?.toString() ?: "")
            et_bitrate.setText(track.bitRate.toString())
            et_samplerate.setText(track.sampleRate.toString())
            et_note.setText(track.note ?: "")
    }

    private fun listenForChanges() {
        et_name.doOnTextChanged{ text, _, _, _ -> newName = text.toString() }
        et_artist.doOnTextChanged{ text, _, _, _ -> newArtist = text.toString() }
        et_featuring.doOnTextChanged{ text, _, _, _ -> newFeaturing = text.toString() }
        et_album.doOnTextChanged{ text, _, _, _ -> newAlbum = text.toString() }
        et_genre.doOnTextChanged{ text, _, _, _ -> newGenre = text.toString() }
        et_track_number.doOnTextChanged{ text, _, _, _ -> newTrackNum = text.toString().toIntOrNull() }
        et_year.doOnTextChanged{ text, _, _, _ -> newYear = text.toString().toIntOrNull() }
        et_note.doOnTextChanged{ text, _, _, _ -> newNote = text.toString() }
    }

    @ExperimentalCoroutinesApi
    private fun update() {

        track?.let {
            val tu = TrackUpdate(
                trackIds = listOf(it.id),
                name = if(it.name != newName) newName.also { hasChanged = true } else null,
                artist = if(it.artist != newArtist) newArtist.also { hasChanged = true } else null,
                featuring = if(it.featuring != newFeaturing) newFeaturing.also { hasChanged = true } else null,
                album = if(it.album != newAlbum)  newAlbum.also { hasChanged = true } else null,
                trackNumber = if(it.trackNumber != newTrackNum) newTrackNum.also { hasChanged = true } else null,
                genre = if(it.genre != newGenre)  newGenre.also { hasChanged = true } else null,
                releaseYear = if(it.releaseYear != newYear)  newYear.also { hasChanged = true } else null,
                note = if(it.note != newNote)  newNote.also { hasChanged = true } else null,
                hidden = null,
                albumArtUrl = null,
                cropArtToSquare = null
            )
            if(hasChanged) {
                //Log.d(TAG, "update: making change!")
                viewModel.setUpdateEvent(UpdateEvent.UpdateTrack(tu))
            } else {
                Toast.makeText(requireContext(), "No changes found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_edit_properties_menu, menu)
        this.menu = menu
    }

    @ExperimentalCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.cancel_action -> {
                findNavController().navigate(R.id.action_trackPropertiesFragment_to_mainFragment)
            }

            R.id.save_action -> {
                update()
            }
        }
        return true
    }

}