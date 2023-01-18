package me.injent.packets.client

import kotlinx.serialization.Serializable
import me.injent.models.Field
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayClientFieldDataChange(
    val field: Field
) : Packet(PacketType.Client.FIELD_DATA_CHANGE)
