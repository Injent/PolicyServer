package me.injent.packets.server

import kotlinx.serialization.Serializable
import me.injent.models.Building
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayServerBuild(
    val x: Int,
    val y: Int,
    val buildingType: Building.Type
) : Packet(PacketType.Server.BUILD)
