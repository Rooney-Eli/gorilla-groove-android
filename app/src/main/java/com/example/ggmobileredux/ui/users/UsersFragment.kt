package com.example.ggmobileredux.ui.users

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.ggmobileredux.R

class UsersFragment : Fragment(R.layout.fragment_users) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        subscribeObservers()
       // viewModel.getUsers()
    }

    private fun setupRecyclerView() {

    }

    private fun subscribeObservers() {

    }


}