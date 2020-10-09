package com.example.ggmobileredux.model

data class LoginResponse(
    var id: Int,
    var token: String,
    var email: String,
    var username: String
)