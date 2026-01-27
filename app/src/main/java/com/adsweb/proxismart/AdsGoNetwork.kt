package com.adsweb.proxismart

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object AdsGoNetwork {
    // Replace with your actual Neon/Hasura URL
    const val ENDPOINT = "https://relaxed-joey-16.hasura.app/v1/graphql"
    private const val ADMIN_SECRET = "4ynayEjBkblp0p3nzmUPpPTd0FDuzkSYLYdSJGSeG1jpAgWzIZfLsuW9dkXJACca"

    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * Extension to add auth headers to Ktor requests
     */
    fun HttpRequestBuilder.hasuraAuth() {
        header("x-hasura-admin-secret", ADMIN_SECRET)
        contentType(ContentType.Application.Json)
    }
}