package com.example.ggmobileredux.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.ggmobileredux.R
import com.example.ggmobileredux.ui.LoginStateEvent
import com.example.ggmobileredux.ui.MainViewModel
import com.example.ggmobileredux.ui.PlayerControlsViewModel
import com.example.ggmobileredux.util.Constants
import com.example.ggmobileredux.util.Constants.KEY_FIRST_TIME_TOGGLE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
    }
}