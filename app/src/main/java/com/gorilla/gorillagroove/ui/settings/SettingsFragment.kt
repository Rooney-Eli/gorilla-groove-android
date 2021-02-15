package com.gorilla.gorillagroove.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.ui.LibraryEvent
import com.gorilla.gorillagroove.ui.MainViewModel
import com.gorilla.gorillagroove.ui.PlayerControlsViewModel
import com.gorilla.gorillagroove.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val viewModel: MainViewModel by viewModels()
    private val controlsViewModel: PlayerControlsViewModel by viewModels()

    @Inject
    lateinit var sharedPref: SharedPreferences

    @set:Inject
    var isFirstAppOpen = true

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logout_button.setOnClickListener {

            controlsViewModel.logout()

           val completed = sharedPref.edit()
                .putBoolean(Constants.KEY_FIRST_TIME_TOGGLE, true)
                .commit() // commit runs synch and returns boolean


            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()

            findNavController().navigate(
                R.id.action_settingsFragment_to_loginFragment,
                savedInstanceState,
                navOptions
            )
        }

        update_tracks_button.setOnClickListener {
            viewModel.setLibraryEvent(LibraryEvent.UpdateAllTracks)
        }

    }
}