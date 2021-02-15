package com.gorilla.gorillagroove.ui.users

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.model.User
import com.gorilla.gorillagroove.ui.MainViewModel
import com.gorilla.gorillagroove.ui.UsersEvent
import com.gorilla.gorillagroove.util.StateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_users.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class UsersFragment : Fragment(R.layout.fragment_users), UserAdapter.OnUserListener {

    val TAG = "AppDebug"
    private val viewModel: MainViewModel by viewModels()
    lateinit var userAdapter: UserAdapter

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        subscribeObservers()
        viewModel.setUsersEvent(UsersEvent.GetAllUsers)
    }

    private fun setupRecyclerView() = users_rv.apply {
        userAdapter = UserAdapter(this@UsersFragment)
        adapter = userAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribeObservers() {
        viewModel.users.observe(requireActivity(), Observer {
            when (it.stateEvent) {
                is StateEvent.Success -> {
//                    displayProgressBar(false)
                    userAdapter.submitList(it.data as List<User>)
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

    override fun onUserClick(position: Int) {
        //Log.d(TAG, "onUserClick: clicked ${userAdapter.userList[position].username}")
    }

    override fun onUserLongClick(position: Int): Boolean {
        //Log.d(TAG, "onUserLongClick: long clicked ${userAdapter.userList[position].username}")
        return true
    }


}