package me.injent.packets.client

import kotlinx.serialization.Serializable
import me.injent.models.Player
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayClientPlayerDataChange(
    val player: Player
) : Packet(PacketType.Client.PLAYER_DATA_CHANGE)
