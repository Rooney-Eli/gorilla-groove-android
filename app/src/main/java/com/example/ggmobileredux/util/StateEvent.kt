package com.example.ggmobileredux.util

sealed class StateEvent{
    object Success: StateEvent()
    object TrackSuccess: StateEvent()
    object TrackListSuccess: StateEvent()
    object AuthSuccess: StateEvent()
    object Error: StateEvent()
    object Loading: StateEvent()
}