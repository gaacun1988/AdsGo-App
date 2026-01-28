package com.adsweb.proxismart

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_profile")
data class LocalProfile(
    @PrimaryKey val id_perfil: String, // Usaremos el UUID que nos de Hasura como llave primaria
    val role: String = "",         // "TIENDA" o "CLIENTE"
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val category: String = "",     // El rubro comercial (ej: "Restaurantes")
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isPremium: Boolean = false,
    val premiumToken: String = "",
    val isLogged: Boolean = true   // Flag para saber si entramos directo
)