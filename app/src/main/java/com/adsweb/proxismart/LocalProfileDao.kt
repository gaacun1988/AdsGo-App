package com.adsweb.proxismart

import androidx.room.*

@Dao
interface LocalProfileDao {
    @Query("SELECT * FROM local_profile LIMIT 1")
    suspend fun getActiveProfile(): LocalProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LocalProfile)

    @Query("DELETE FROM local_profile")
    suspend fun clearProfile()
}