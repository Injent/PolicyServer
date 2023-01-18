package me.injent.packets

import io.ktor.websocket.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import me.injent.extensions.jsonFormat
import me.injent.models.DEBUG
import me.injent.models.PolicyGame
import me.injent.packets.client.PlayClientGameState
import me.injent.packets.server.PlayServerAttack
import me.injent.packets.server.PlayServerBuild
import me.injent.packets.server.PlayServerCaptureField
import me.injent.packets.server.PlayServerMineResource

/**
 * Constructs a new strongly typed wrapper for the given packet.
 *
 * @param raw - handle to the raw packet data.
 * @param type - the packet id.
 */

@Serializable
abstract class Packet(@SerialName("packetId") val packetType: Int) {

    suspend fun send(receiver: String) {
        val json = jsonFormat.encodeToString(ClientPacketSerializer, this)
        PolicyGame.sockets[receiver]?.send(json)
        if (DEBUG) println(json)
    }

    suspend fun broadcast() {
        val json = jsonFormat.encodeToString(ClientPacketSerializer, this)
        for (socket in PolicyGame.sockets.values) {
            socket.send(json)
        }
        if (DEBUG) println(json)
    }
}

object PacketType {
    object Server {
        const val CAPTURE_FIELD = 0
        const val MINE = 1
        const val BUILD = 2
        const val IDLE = 3
        const val ATTACK = 4
    }

    object Client {
        const val GAME_STATE = 0
        const val ALERT = 1
        const val SOUND = 2
        const val FIELD_DATA_CHANGE = 3
        const val TURN_STATE = 4
        const val PLAYER_DATA_CHANGE = 5
    }
}

object ServerPacketSerializer : JsonContentPolymorphicSerializer<Packet>(Packet::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out Packet> {
        return when (element.jsonObject["packetId"].toString().toIntOrNull()) {
            PacketType.Server.CAPTURE_FIELD -> PlayServerCaptureField.serializer()
            PacketType.Server.BUILD -> PlayServerBuild.serializer()
            PacketType.Server.MINE -> PlayServerMineResource.serializer()
            PacketType.Server.ATTACK -> PlayServerAttack.serializer()
            else -> Packet.serializer()
        }
    }
}

object ClientPacketSerializer : JsonContentPolymorphicSerializer<Packet>(Packet::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out Packet> {
        return when (element.jsonObject["packetId"].toString().toIntOrNull()) {
            PacketType.Client.GAME_STATE -> PlayClientGameState.serializer()
            else -> Packet.serializer()
        }
    }
}