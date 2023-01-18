package me.injent.packets.client

import kotlinx.serialization.Serializable
import me.injent.models.TurnState
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayClientTurnState(
    val turnState: TurnState
) : Packet(PacketType.Client.TURN_STATE)
