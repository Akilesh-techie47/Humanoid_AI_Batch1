package com.humanoidai.recovery.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GapEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gap: GapEventEntity)

    @Update
    suspend fun update(gap: GapEventEntity)

    @Query("SELECT * FROM gap_events WHERE acknowledged = 0 ORDER BY gapStart DESC")
    fun getUnacknowledgedGaps(): Flow<List<GapEventEntity>>

    @Query("SELECT * FROM gap_events ORDER BY gapStart DESC LIMIT :limit")
    suspend fun getRecentGaps(limit: Int = 20): List<GapEventEntity>

    @Query("UPDATE gap_events SET acknowledged = 1 WHERE gapId = :gapId")
    suspend fun acknowledge(gapId: String)

    @Query("SELECT * FROM gap_events ORDER BY gapEnd DESC LIMIT 1")
    suspend fun getMostRecentGap(): GapEventEntity?
}
