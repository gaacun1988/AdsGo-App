package com.adsweb.proxismart

import androidx.room.*

@Dao
interface OfferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalProfile(profile: LocalProfile)

    @Query("SELECT * FROM local_profiles ORDER BY id DESC")
    suspend fun getAllLocalProfiles(): List<LocalProfile>

    @Query("DELETE FROM local_profiles")
    suspend fun deleteProfile()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: Offer)

    @Query("SELECT * FROM offers_table ORDER BY creationTime DESC")
    suspend fun getAllOffers(): List<Offer>
}