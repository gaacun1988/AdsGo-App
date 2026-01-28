package com.adsweb.proxismart

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Offer::class, LocalProfile::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun offerDao(): OfferDao
    // AGREGA ESTO: Sin el DAO de perfil, no podemos guardar la sesión
    abstract fun localProfileDao(): LocalProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adsgo_final_engine_v9"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}