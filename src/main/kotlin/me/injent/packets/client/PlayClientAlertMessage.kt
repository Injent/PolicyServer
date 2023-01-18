package me.injent.packets.client

import kotlinx.serialization.Serializable
import me.injent.models.Sound
import me.injent.packets.Packet
import me.injent.packets.PacketType

@Serializable
data class PlayClientAlertMessage(
    val message: String,
    val sound: String? = Sound.POPUP_ENTER
) : Packet(PacketType.Client.ALERT)