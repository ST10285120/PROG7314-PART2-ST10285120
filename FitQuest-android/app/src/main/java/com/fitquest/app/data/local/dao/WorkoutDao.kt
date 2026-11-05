package com.fitquest.app.data.local.dao

import androidx.room.*
import com.fitquest.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

/** Room access to workouts. Writing here first is what makes logging work offline. */
@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: WorkoutEntity)

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE synced = 0")
    suspend fun getUnsynced(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE completedAt IS NOT NULL ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecentCompleted(limit: Int = 10): Flow<List<WorkoutEntity>>

    /** XP estimates for finished workouts the server has not confirmed yet. */
    @Query("SELECT COALESCE(SUM(xpEarned), 0) FROM workouts WHERE completedAt IS NOT NULL AND synced = 0")
    fun observePendingXp(): Flow<Int>

    @Query("UPDATE workouts SET synced = 0 WHERE localId = :localId")
    suspend fun markUnsynced(localId: String)

    @Query("SELECT MAX(completedAt) FROM workouts")
    suspend fun getLatestCompletedAt(): Long?

    @Query("SELECT COUNT(*) FROM workouts WHERE synced = 0")
    suspend fun countUnsynced(): Int

    @Query("DELETE FROM workouts WHERE localId = :localId")
    suspend fun delete(localId: String)

    /** Child rows go too, because the foreign keys cascade. */
    @Query("DELETE FROM workouts")
    suspend fun deleteAll()
}
