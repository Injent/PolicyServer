package me.injent.packets.server

import kotlinx.serialization.Serializable
import me.injent.models.Resource
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayServerMineResource(
    val x: Int,
    val y: Int,
    val resource: Resource.Type
) : Packet(PacketType.Server.MINE)
