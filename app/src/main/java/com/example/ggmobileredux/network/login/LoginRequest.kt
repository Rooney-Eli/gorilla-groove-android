package com.example.ggmobileredux.network.login

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
    val version: String,
    val deviceType: String
)