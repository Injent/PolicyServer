package me.injent.data

sealed interface ResultOf {
    data class Error(val message: String) : ResultOf
    object Success : ResultOf
}