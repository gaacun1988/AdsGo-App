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
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Entity(tableName = "offers_table")
data class Offer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storeName: String = "",
    val title: String = "",
    val price: String = "",
    val category: String = "",
    val subCategory: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 100f,
    val whatsapp: String = "",
    val views: Int = 0,
    val clicks: Int = 0,
    val creationTime: Long = System.currentTimeMillis()
) {
    fun getExpirationDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(creationTime + (24 * 3600000L)))
    }
}