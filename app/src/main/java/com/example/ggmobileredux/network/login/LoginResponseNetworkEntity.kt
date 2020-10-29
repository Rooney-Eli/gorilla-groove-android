package com.example.ggmobileredux.network.login

data class LoginResponseNetworkEntity(
    var id: Int,
    var token: String,
    var email: String,
    var username: String
)