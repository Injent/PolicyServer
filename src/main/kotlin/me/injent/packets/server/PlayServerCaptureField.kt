package me.injent.packets.server

import kotlinx.serialization.Serializable
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayServerCaptureField(
    val x: Int, val y: Int
) : Packet(PacketType.Server.CAPTURE_FIELD)
