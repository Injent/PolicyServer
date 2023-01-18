package me.injent.packets.client

import kotlinx.serialization.Serializable
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayClientSound(
    val sound: String
) : Packet(PacketType.Client.SOUND)
