package com.adsweb.proxismart

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// MODELOS PARA RESPUESTAS DE HASURA
@Serializable
data class GraphQLRequest(val query: String)

@Serializable
data class RegisterResponse(val data: RegisterData? = null)

@Serializable
data class RegisterData(val insert_perfil_one: PerfilId?)

@Serializable
data class PerfilId(val id_perfil: String)

object AdsGoNetwork {
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

    private fun HttpRequestBuilder.hasuraAuth() {
        header("x-hasura-admin-secret", ADMIN_SECRET)
        contentType(ContentType.Application.Json)
    }

    /**
     * REGISTRO INTEGRADO: Crea el Perfil y la Tienda en un solo paso
     */
    suspend fun registerStoreWithProfile(
        email: String,
        storeName: String,
        idCategory: Int,
        lat: Double,
        lng: Double
    ): Result<String> {
        // Mutación que sigue tu esquema de la imagen (perfil -> tienda)
        val mutation = """
            mutation RegisterStore {
              insert_perfil_one(object: {
                email: "$email",
                tipo_usuario: "comercio",
                tiendas: {
                  data: {
                    nombre: "$storeName",
                    id_tipo_comercio: $idCategory,
                    id_plan: 1,
                    ubicacion: {
                      type: "Point",
                      coordinates: [$lng, $lat]
                    }
                  }
                }
              }) {
                id_perfil
              }
            }
        """.trimIndent()

        return try {
            val response: RegisterResponse = httpClient.post(ENDPOINT) {
                hasuraAuth()
                setBody(GraphQLRequest(query = mutation))
            }.body()

            val newId = response.data?.insert_perfil_one?.id_perfil
            if (newId != null) {
                Result.success(newId)
            } else {
                Result.failure(Exception("No se recibió ID de perfil"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}