package com.fitquest.app.data.local.dao

import androidx.room.*
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

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

@Dao
interface SetEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setEntry: SetEntryEntity)

    @Query("SELECT * FROM set_entries WHERE workoutExerciseLocalId = :weLocalId ORDER BY setNumber ASC")
    fun observeForWorkoutExercise(weLocalId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE workoutExerciseLocalId = :weLocalId ORDER BY setNumber ASC")
    suspend fun getForWorkoutExercise(weLocalId: String): List<SetEntryEntity>

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

data class PersonalRecordRow(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val achievedAt: Long
)
