package me.injent.models

import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.injent.data.ResultOf
import me.injent.packets.Packet
import me.injent.packets.PacketType
import me.injent.packets.client.*
import me.injent.packets.server.PlayServerAttack
import me.injent.packets.server.PlayServerBuild
import me.injent.packets.server.PlayServerCaptureField
import me.injent.packets.server.PlayServerMineResource
import java.util.concurrent.ConcurrentHashMap

const val DEBUG = true

class PolicyGame {

    private val state = GameState(size = 9)

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val sockets = ConcurrentHashMap<String, WebSocketSession>()
    }

    fun onPacketReceived(playerName: String, packet: Packet) {
        val player = state.connectedPlayers[playerName] ?: return

        if (state.turnState.playerAtTurn != player.name) {
            gameScope.launch {
                alertPacket("Сейчас не ваш ход").send(playerName)
            }
            return
        }

        when (packet.packetType) {
            PacketType.Server.CAPTURE_FIELD -> captureField(player, packet as PlayServerCaptureField)
            PacketType.Server.MINE -> mineResource(player, packet as PlayServerMineResource)
            PacketType.Server.BUILD -> build(player, packet as PlayServerBuild)
            PacketType.Server.IDLE -> skipTurn(player)
            PacketType.Server.ATTACK -> attack(player, packet as PlayServerAttack)
        }
    }

    private fun captureField(player: Player, packet: PlayServerCaptureField) {
        val field = state.fields[packet.y][packet.x]
        val result = field.capture(state.fields, player)

        if (result is ResultOf.Error) {
            val alertPacket = PlayClientAlertMessage(result.message)
            gameScope.launch {
                alertPacket.send(player.name)
            }
            return
        }

        updateField(field)
        updatePlayer(player)
        val fieldPacket = PlayClientFieldDataChange(field)
        val playerPacket = PlayClientPlayerDataChange(player)
        val soundPacket = PlayClientSound(Sound.CONFIRM)

        gameScope.launch {
            fieldPacket.broadcast()
            playerPacket.send(player.name)
            soundPacket.send(player.name)
        }
        endTurn(player)
    }

    private fun mineResource(player: Player, packet: PlayServerMineResource) {
        val field = state.fields[packet.y][packet.x]
        val result = field.mineResource(player, packet.resource)

        if (result is ResultOf.Error) {
            val alertPacket = PlayClientAlertMessage(result.message, Sound.FAILED)
            gameScope.launch {
                alertPacket.send(player.name)
            }
            return
        }

        updateField(field)
        updatePlayer(player)
        val fieldPacket = PlayClientFieldDataChange(field)
        val playerPacket = PlayClientPlayerDataChange(player)
        val sound = if (packet.resource == Resource.Type.WOOD) Sound.WOOD else Sound.STONE
        val soundPacket = PlayClientSound(sound)

        gameScope.launch {
            fieldPacket.broadcast()
            playerPacket.send(player.name)
            soundPacket.send(player.name)
        }
        endTurn(player)
    }

    private fun build(player: Player, packet: PlayServerBuild) {
        val field = state.fields[packet.y][packet.x]
        val errorBuildPacket = field.build(player, packet.buildingType)

        if (errorBuildPacket != null) {
            gameScope.launch {
                errorBuildPacket.send(player.name)
            }
            return
        }

        updateField(field)
        updatePlayer(player)
        val fieldPacket = PlayClientFieldDataChange(field)
        val playerPacket = PlayClientPlayerDataChange(player)
        val soundPacket = PlayClientSound(Sound.BUILD)

        gameScope.launch {
            fieldPacket.broadcast()
            playerPacket.send(player.name)
            soundPacket.broadcast()
        }
        endTurn(player)
    }

    private fun attack(player: Player, packet: PlayServerAttack) {
        val field = state.fields[packet.fromY][packet.fromX]
        val attackedField = state.fields[packet.attackedY][packet.attackedX]
        val attackedPlayer = state.connectedPlayers[attackedField.owner]

        val errorPacket = field.attack(state.fields, player, attackedPlayer, attackedField)
        if (errorPacket != null) {
            gameScope.launch {
                errorPacket.send(player.name)
            }
            return
        }

        updateField(field)
        updateField(attackedField)
        updatePlayer(player)
        updatePlayer(attackedPlayer)
        val playerPacket = PlayClientPlayerDataChange(player)
        val attackedPlayerPacket = PlayClientPlayerDataChange(player)
        val fieldPacket = PlayClientFieldDataChange(field)
        val attackedFieldPacket = PlayClientFieldDataChange(attackedField)
        val soundPacket = PlayClientSound(Sound.DEFEND)

        gameScope.launch {
            playerPacket.broadcast()
            attackedPlayerPacket.broadcast()
            fieldPacket.broadcast()
            attackedFieldPacket.broadcast()
            soundPacket.broadcast()
        }
        endTurn(player)
    }

    fun connectPlayer(name: String, rgb: String, session: WebSocketSession): Player? {
        val statePacket = PlayClientGameState(state)
        if (state.connectedPlayers.containsKey(name)) {
            if (!sockets.containsKey(name)) {
                sockets[name] = session
            }

            state.connectedPlayers.apply {
                this[name] = this[name]?.copy(disconnected = false) ?: return@apply
            }
            val alertPacket = alertPacket("$name вернулся в игру")

            gameScope.launch {
                statePacket.send(name)
                alertPacket.broadcast()
            }

            return state.connectedPlayers[name]
        }
        sockets[name] = session

        val player = Player(name = name, rgb = rgb.toIntOrNull() ?: 0, houses = 1)
        val playerField = Field.getStartField(player, state.fields)

        state.apply {
            turnState.turnSequence = turnState.turnSequence.plus(name).shuffled()
            turnState.playerAtTurn = turnState.turnSequence.first()
            connectedPlayers.apply {
                this[name] = player
            }
            fields.apply {
                get(playerField.y)[playerField.x] = playerField
            }
        }

        val alertPacket = alertPacket("$name присоединился к игре")

        gameScope.launch {
            statePacket.send(name)
            alertPacket.broadcast()
        }
        return player
    }

    fun disconnectPlayer(playerName: String) {
        sockets.remove(playerName)
        state.turnState.playerAtTurn = if (state.turnState.playerAtTurn == playerName) whoNextAfter(playerName)
            else state.turnState.playerAtTurn
        state.connectedPlayers[playerName]?.copy(disconnected = false)?.let {
            state.connectedPlayers[playerName] = it
        }

        val alertPacket = alertPacket("$playerName покинул игру")
        gameScope.launch {
            alertPacket.broadcast()
        }
    }

    private fun whoNextAfter(playerName: String): String {
        val currentPlayerIndex = state.turnState.turnSequence.indexOf(playerName)
        val nextPlayer = try {
            var name: String = state.turnState.turnSequence[currentPlayerIndex + 1]
            var time = 1
            while (state.connectedPlayers[name]?.disconnected != false) {
                time++
                name = state.turnState.turnSequence[currentPlayerIndex + time]
                if (state.connectedPlayers[name]?.disconnected == false)
                    break
            }
            return name
        } catch (e: IndexOutOfBoundsException) {
            state.turnState.turnSequence[0]
        }
        return nextPlayer
    }

    private fun endTurn(player: Player) {
        state.turnState.turn++
        val nextPlayer = if (state.turnState.turn == 3) {
            state.turnState.turn = 0
            whoNextAfter(player.name)
        } else {
            player.name
        }
        val nextPlayerObject: Player? = if (state.turnState.turn == 0)
            state.connectedPlayers[nextPlayer]
        else null

        state.turnState.playerAtTurn = nextPlayer

        val turnPacket = PlayClientTurnState(state.turnState)
        gameScope.launch {
            turnPacket.broadcast()

            nextPlayerObject?.apply {
                passiveAction(state.fields, this)
            }
        }
    }

    private fun skipTurn(player: Player) {
        state.turnState.apply {
            playerAtTurn = whoNextAfter(player.name)
            nextPlayer = whoNextAfter(playerAtTurn!!)
            turn = 0
        }
        gameScope.launch {
            alertPacket("Ход передан другому").send(player.name)
        }
    }

    private suspend fun passiveAction(fields: Array<Array<Field>>, player: Player) {
        fields.forEach { row ->
            row.forEach { field ->
                if (field.owner == player.name) {

                    // Auto grinders
                    field.buildings.forEach { building ->
                        when (building.type) {
                            Building.Type.MINESHAFT -> {
                                for (resource in field.resources.shuffled()) {
                                    if (resource.count > 0 && resource.isMineable) {
                                        field.mineResource(player, resource.type)
                                        break
                                    }
                                }
                            }
                            Building.Type.SAWMILL -> {
                                for (resource in field.resources.shuffled()) {
                                    if (resource.count > 0 && !resource.isMineable) {
                                        field.mineResource(player, resource.type)
                                        break
                                    }
                                }
                            }
                            Building.Type.BANK -> {
                                player.addResource(Resource.Type.GOLD)
                            }
                            else -> {}
                        }
                    }
                    updateField(field)
                    PlayClientFieldDataChange(field).broadcast()
                }
            }
        }
    }

    private fun updatePlayer(player: Player?) {
        if (player == null) return
        state.connectedPlayers[player.name] = player
    }

    private fun updateField(field: Field) {
        state.fields[field.y][field.x] = field
    }
}

fun alertPacket(message: String, sound: String? = null): Packet {
    return PlayClientAlertMessage(message, sound)
}