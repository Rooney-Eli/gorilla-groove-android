package com.gorilla.gorillagroove.model

data class LoginResponse(
    var id: Long,
    var token: String,
    var email: String,
    var username: String
)