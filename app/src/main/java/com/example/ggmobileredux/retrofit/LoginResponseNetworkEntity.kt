package com.example.ggmobileredux.retrofit

data class LoginResponseNetworkEntity(
    var id: Int,
    var token: String,
    var email: String,
    var username: String
)