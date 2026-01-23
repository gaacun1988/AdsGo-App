package com.adsweb.proxismart

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "local_profiles")
data class LocalProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String = "",
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val category: String = "",
    val subCategory: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isPremium: Boolean = false,
    val premiumToken: String = ""
)

@Entity(tableName = "offers")
data class Offer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storeName: String = "",
    val storeLogo: String = "",
    val title: String = "",
    val price: String = "",
    val category: String = "",
    val subCategory: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isPremium: Boolean = false,
    val radius: Float = 100f,
    val whatsapp: String = "",
    val views: Int = 0,
    val clicks: Int = 0,
    val creationTime: Long = System.currentTimeMillis(),
    val durationHours: Int = 24
) {
    fun getExpirationDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(creationTime + (durationHours * 3600000L)))
    }
}