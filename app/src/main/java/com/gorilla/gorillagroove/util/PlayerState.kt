package com.gorilla.gorillagroove.util

import java.lang.Exception

sealed class PlayerState<out R> {
    data class Playing<out T>(val data: T): PlayerState<T>()
    data class Error(val exception: Exception): PlayerState<Nothing>()
    object Stopped: PlayerState<Nothing>()
}