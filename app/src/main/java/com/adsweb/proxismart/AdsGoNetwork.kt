package com.adsweb.proxismart

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object AdsGoNetwork {
    // Motor de peticiones HTTP (Ktor)
    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    // PEGA AQUÍ LOS DATOS QUE SACASTE DE HASURA
    // El Endpoint termina en /v1/graphql
    const val ENDPOINT = "https://relaxed-joey-16.hasura.app/v1/graphql"
    const val ADMIN_SECRET = "4ynayEjBkblp0p3nzmUPpPTd0FDuzkSYLYdSJGSeG1jpAgWzIZfLsuW9dkXJACca"

    // Función para autorizar cada llamada a la base de datos Neon
    fun HttpRequestBuilder.hasuraAuth() {
        header("x-hasura-admin-secret", ADMIN_SECRET)
        header("Content-Type", "application/json")
    }
}