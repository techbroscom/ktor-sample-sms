package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://dpg-d0q8diqli9vc73b9bhmg-a.singapore-postgres.render.com/sms_oeun",
        user = "sms",
        driver = "org.postgresql.Driver",
        password = "eZmti5NHOvT5jqrvK4k4rIAx5BF8v9Hd",
    )

    val jsonFormatter = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val userService = UserService(database)

    routing {
        route("/users") {
            post {
                val user = call.receive<ExposedUser>()
                val id = userService.create(user)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@get
                }

                val user = userService.read(id)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@put
                }

                val user = call.receive<ExposedUser>()
                userService.update(id, user)
                call.respond(HttpStatusCode.OK, mapOf("message" to "User updated"))
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@delete
                }

                userService.delete(id)
                call.respond(HttpStatusCode.OK, mapOf("message" to "User deleted"))
            }

            get {
                val users = userService.getAll()
                call.respond(jsonFormatter.encodeToString(users))
            }
        }
    }
}
