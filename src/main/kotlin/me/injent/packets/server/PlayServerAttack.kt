package me.injent.packets.server

import kotlinx.serialization.Serializable
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayServerAttack(
    val fromX: Int,
    val fromY: Int,
    val attackedX: Int,
    val attackedY: Int
) : Packet(PacketType.Server.ATTACK)