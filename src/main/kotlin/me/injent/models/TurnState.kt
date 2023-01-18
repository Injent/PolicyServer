package me.injent.models

import kotlinx.serialization.Serializable

@Serializable
data class TurnState(
    var turn: Int = 0,
    var playerAtTurn: String? = null,
    var nextPlayer: String? = null,
    var turnSequence: List<String> = emptyList()
)
