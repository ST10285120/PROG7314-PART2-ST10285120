package com.fitquest.app.data.local.dao

import androidx.room.*
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

/** Room access to the exercises inside a workout. */
@Dao
interface WorkoutExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workoutExercise: WorkoutExerciseEntity)

    @Query("SELECT * FROM workout_exercises WHERE workoutLocalId = :workoutLocalId ORDER BY orderIndex ASC")
    fun observeForWorkout(workoutLocalId: String): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutLocalId = :workoutLocalId ORDER BY orderIndex ASC")
    suspend fun getForWorkout(workoutLocalId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): WorkoutExerciseEntity?
}

/** Room access to logged sets. */
@Dao
interface SetEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setEntry: SetEntryEntity)

    @Query("SELECT * FROM set_entries WHERE workoutExerciseLocalId = :weLocalId ORDER BY setNumber ASC")
    fun observeForWorkoutExercise(weLocalId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE workoutExerciseLocalId = :weLocalId ORDER BY setNumber ASC")
    suspend fun getForWorkoutExercise(weLocalId: String): List<SetEntryEntity>

    @Query("DELETE FROM set_entries WHERE localId = :localId")
    suspend fun delete(localId: String)

    /** Replaces the local PR estimate with the server's decision. */
    @Query("UPDATE set_entries SET isPersonalRecord = :isPersonalRecord WHERE localId = :localId")
    suspend fun updatePersonalRecord(localId: String, isPersonalRecord: Boolean)

    @Query(
        """
        SELECT COUNT(*) FROM set_entries
        WHERE workoutExerciseLocalId IN (
            SELECT localId FROM workout_exercises WHERE workoutLocalId = :workoutLocalId
        )
        """
    )
    fun observeSetCount(workoutLocalId: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM set_entries
        WHERE isPersonalRecord = 1 AND workoutExerciseLocalId IN (
            SELECT localId FROM workout_exercises WHERE workoutLocalId = :workoutLocalId
        )
        """
    )
    suspend fun countPersonalRecords(workoutLocalId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM set_entries
        WHERE workoutExerciseLocalId IN (
            SELECT localId FROM workout_exercises WHERE workoutLocalId = :workoutLocalId
        )
        """
    )
    suspend fun countSets(workoutLocalId: String): Int

    @Query("SELECT MAX(weightKg) FROM set_entries WHERE workoutExerciseLocalId IN (SELECT localId FROM workout_exercises WHERE exerciseId = :exerciseId)")
    suspend fun getBestWeightForExercise(exerciseId: String): Double?

    @Query(
        """
        SELECT SUM(reps * weightKg) FROM set_entries
        WHERE workoutExerciseLocalId IN (
            SELECT localId FROM workout_exercises WHERE workoutLocalId = :workoutLocalId
        )
        """
    )
    suspend fun getTotalVolumeForWorkout(workoutLocalId: String): Double?

    @Query(
        """
        SELECT we.exerciseName AS exerciseName, se.weightKg AS weightKg, se.reps AS reps,
               w.startedAt AS achievedAt
        FROM set_entries se
        JOIN workout_exercises we ON se.workoutExerciseLocalId = we.localId
        JOIN workouts w ON we.workoutLocalId = w.localId
        WHERE se.isPersonalRecord = 1
        ORDER BY w.startedAt DESC
        """
    )
    fun observeAllPersonalRecords(): Flow<List<PersonalRecordRow>>
}

/** A personal record joined with its exercise name, for offline display. */
data class PersonalRecordRow(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val achievedAt: Long
)
