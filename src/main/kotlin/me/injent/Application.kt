package me.injent

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.injent.models.PolicyGame
import me.injent.plugins.configureMonitoring
import me.injent.plugins.configureRouting
import me.injent.plugins.configureSerialization
import me.injent.plugins.configureSockets

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, host = "26.20.11.247", port = 25565) {
        module()
    }
    server.start(wait = true)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val game = PolicyGame()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureRouting(game)
}
