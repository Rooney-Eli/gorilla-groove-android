package com.example.ggmobileredux.util

data class DataState<T>(
    var data: T? = null,
    var stateEvent: StateEvent? = null
)


