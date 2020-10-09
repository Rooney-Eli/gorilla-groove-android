package com.example.ggmobileredux.util

data class SessionState<T>(
    var data: T? = null,
    var stateEvent: StateEvent? = null
)