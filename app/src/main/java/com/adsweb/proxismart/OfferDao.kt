package com.adsweb.proxismart

import androidx.room.*

@Dao
interface OfferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalProfile(profile: LocalProfile)

    @Query("SELECT * FROM local_profiles ORDER BY id DESC")
    suspend fun getAllLocalProfiles(): List<LocalProfile>

    @Query("DELETE FROM local_profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: Offer)

    @Query("SELECT * FROM offers ORDER BY creationTime DESC")
    suspend fun getAllOffers(): List<Offer>
}