package com.example.ggmobileredux.util

sealed class StateEvent{
    object Success: StateEvent()
    object Loading: StateEvent()
    object AuthSuccess: StateEvent()
    object Error: StateEvent()
}