package me.injent.packets.client

import kotlinx.serialization.Serializable
import me.injent.models.GameState
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayClientGameState(
    val gameState: GameState
) : Packet(PacketType.Client.GAME_STATE)
