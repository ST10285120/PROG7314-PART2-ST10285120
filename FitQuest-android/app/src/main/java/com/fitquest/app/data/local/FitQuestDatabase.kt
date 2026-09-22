package com.fitquest.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fitquest.app.data.local.dao.SetEntryDao
import com.fitquest.app.data.local.dao.WorkoutDao
import com.fitquest.app.data.local.dao.WorkoutExerciseDao
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [WorkoutEntity::class, WorkoutExerciseEntity::class, SetEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FitQuestDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        @Volatile private var INSTANCE: FitQuestDatabase? = null

        fun getInstance(context: Context): FitQuestDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitQuestDatabase::class.java,
                    "fitquest.db"
                ).build().also { INSTANCE = it }
            }
    }
}
