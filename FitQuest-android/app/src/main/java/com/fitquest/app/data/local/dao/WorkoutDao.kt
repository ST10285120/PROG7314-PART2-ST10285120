package com.fitquest.app.data.local.dao

import androidx.room.*
import com.fitquest.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: WorkoutEntity)

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): WorkoutEntity?

    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE synced = 0")
    suspend fun getUnsynced(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE completedAt IS NOT NULL ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecentCompleted(limit: Int = 10): Flow<List<WorkoutEntity>>

    @Query("SELECT COALESCE(SUM(xpEarned), 0) FROM workouts WHERE completedAt IS NOT NULL")
    fun observeTotalXp(): Flow<Int>
}
