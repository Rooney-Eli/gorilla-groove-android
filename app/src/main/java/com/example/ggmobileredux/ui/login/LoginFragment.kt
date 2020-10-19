package com.example.ggmobileredux.ui.login

import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings.Secure
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.ggmobileredux.R
import com.example.ggmobileredux.retrofit.LoginRequest
import com.example.ggmobileredux.retrofit.LoginResponseNetworkEntity
import com.example.ggmobileredux.ui.library.LoginStateEvent
import com.example.ggmobileredux.ui.library.MainActivity
import com.example.ggmobileredux.ui.library.MainViewModel
import com.example.ggmobileredux.util.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.ggmobileredux.util.StateEvent
import com.example.ggmobileredux.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var sharedPref: SharedPreferences

    @set:Inject
    var isFirstAppOpen = true

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!isFirstAppOpen) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.mainFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_loginFragment_to_mainFragment,
                savedInstanceState,
                navOptions
            )
        }


        edit_text_email.requestFocus()

        button_login.setOnClickListener {
            val deviceId = fetchDeviceUUID()
            val loginRequest = LoginRequest(edit_text_email.text.toString(), edit_text_password.text.toString(), deviceId.toString(), "", "ANDROID")


            viewModel.setLoginStateEvent(LoginStateEvent.LoginEvent(loginRequest))

        }
        subscribeObservers()
    }

    private fun fetchDeviceUUID(): UUID {
        val androidId = Secure.getString(context?.getContentResolver(), Secure.ANDROID_ID);
        return UUID.nameUUIDFromBytes(androidId.toByteArray(charset("utf8")))
    }

    private fun subscribeObservers() {
        viewModel.loginState.observe(requireActivity(), Observer {
            when(it.stateEvent) {
                is StateEvent.AuthSuccess -> {
                    displayProgressBar(false)
                    val response = it.data as LoginResponseNetworkEntity
                    //viewModel. = response.token
                    hideKeyboard(activity as MainActivity)

                    writePersonalDataToSharedPref()

                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }
                is StateEvent.Loading -> {
                    displayProgressBar(true)
                }
                is StateEvent.Error -> {
                    displayProgressBar(false)
                    Toast.makeText(requireContext(), "Invalid Login", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun displayProgressBar(isDisplayed: Boolean) {
        if(isDisplayed){
            progress_bar_login.visibility = View.VISIBLE
            progress_bar_login.bringToFront()
        } else {
            progress_bar_login.visibility =  View.GONE
        }
    }

    private fun writePersonalDataToSharedPref() {

        sharedPref.edit()
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false)
            .apply()

    }

}