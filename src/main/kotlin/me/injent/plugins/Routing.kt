package me.injent.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import me.injent.models.PolicyGame
import me.injent.socket

fun Application.configureRouting(game: PolicyGame) {
    routing {
        socket(game)
    }
}
