package me.injent

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import me.injent.models.PolicyGame
import me.injent.packets.ServerPacketSerializer
import kotlin.random.Random

fun Route.socket(game: PolicyGame) {
    route("/play/{name}/{country}", HttpMethod.Get) {
        webSocket {
            val name = call.parameters["name"] ?: "Player-${Random.nextInt(0, Int.MAX_VALUE)}"
            val country = call.parameters["country"] ?: "none"
            println("$name joined the game")
            val player = game.connectPlayer(name, country, this)

            if (player == null) {
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        "Error"
                    )
                )
            }

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val packet = Json.decodeFromString(ServerPacketSerializer, frame.readText())

                        if (player?.name != null)
                            game.onPacketReceived(name, packet)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                player?.name?.let {
                    println("$it quit the game")
                    game.disconnectPlayer(it)
                }
            }
        }
    }
}