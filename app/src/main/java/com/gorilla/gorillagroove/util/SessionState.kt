package com.gorilla.gorillagroove.util

data class SessionState<T>(
    var data: T? = null,
    var stateEvent: StateEvent? = null
)