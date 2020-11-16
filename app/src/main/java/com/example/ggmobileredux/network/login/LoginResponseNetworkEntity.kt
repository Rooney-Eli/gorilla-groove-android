package com.example.ggmobileredux.network.login

data class LoginResponseNetworkEntity(
    var id: Long,
    var token: String,
    var email: String,
    var username: String
)