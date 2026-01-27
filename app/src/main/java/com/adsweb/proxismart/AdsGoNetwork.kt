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

// MODELOS DE DATOS
@Serializable
data class GraphQLRequest(val query: String)

@Serializable
data class RegisterResponse(val data: RegisterData? = null)

@Serializable
data class RegisterData(val insert_perfil_one: PerfilId?)

@Serializable
data class PerfilId(val id_perfil: String)

@Serializable
data class StoreStatsResponse(val data: StoreStatsData? = null)

@Serializable
data class StoreStatsData(val tienda: List<TiendaStats>? = null)

@Serializable
data class TiendaStats(
    val vistas: Int = 0,
    val clics: Int = 0,
    val is_premium: Boolean = false,
    val radius: Int = 100
)
@Serializable
data class NearbyStoresResponse(val data: NearbyStoresData? = null)
@Serializable
data class NearbyStoresData(val tienda: List<RemoteStore>)
@Serializable
data class RemoteStore(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val id_plan: Int,
    val ubicacion: String // Formato GeoJSON que devuelve PostGIS
)
object AdsGoNetwork {
    const val ENDPOINT = "https://relaxed-joey-16.hasura.app/v1/graphql"
    private const val ADMIN_SECRET = "4ynayEjBkblp0p3nzmUPpPTd0FDuzkSYLYdSJGSeG1jpAgWzIZfLsuW9dkXJACca"

    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; prettyPrint = true })
        }
    }
    suspend fun fetchNearbyStores(lat: Double, lng: Double): Result<List<RemoteStore>> {
        val query = """
        query GetNearbyStores {
          tienda(where: {
            ubicacion: { _st_d_within: { 
                distance: 300, 
                from: { type: "Point", coordinates: [$lng, $lat] } 
            } }
          }) {
            id
            nombre
            categoria
            id_plan
            ubicacion
          }
        }
    """.trimIndent()

        return try {
            val res: NearbyStoresResponse = httpClient.post(ENDPOINT) {
                hasuraAuth()
                setBody(GraphQLRequest(query))
            }.body()
            Result.success(res.data?.tienda ?: emptyList())
        } catch(e: Exception) { Result.failure(e) }
    }
    private fun HttpRequestBuilder.hasuraAuth() {
        header("x-hasura-admin-secret", ADMIN_SECRET)
        contentType(ContentType.Application.Json)
    }

    suspend fun registerStoreWithProfile(email: String, storeName: String, idCategory: Int, lat: Double, lng: Double): Result<String> {
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
                    ubicacion: { type: "Point", coordinates: [$lng, $lat] }
                  }
                }
              }) { id_perfil }
            }
        """.trimIndent()

        return try {
            val response: RegisterResponse = httpClient.post(ENDPOINT) {
                hasuraAuth(); setBody(GraphQLRequest(mutation))
            }.body()
            val id = response.data?.insert_perfil_one?.id_perfil
            if (id != null) Result.success(id) else Result.failure(Exception("Error"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchStoreStats(idPerfil: String): Result<TiendaStats> {
        val query = """
            query GetStats {
              tienda(where: {id_perfil: {_eq: "$idPerfil"}}) {
                vistas
                clics
                is_premium
                radius
              }
            }
        """.trimIndent()

        return try {
            val res: StoreStatsResponse = httpClient.post(ENDPOINT) {
                hasuraAuth(); setBody(GraphQLRequest(query))
            }.body()
            Result.success(res.data?.tienda?.firstOrNull() ?: TiendaStats())
        } catch (e: Exception) { Result.failure(e) }
    }
}