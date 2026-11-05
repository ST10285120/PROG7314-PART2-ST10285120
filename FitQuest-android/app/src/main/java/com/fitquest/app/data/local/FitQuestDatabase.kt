package com.fitquest.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitquest.app.data.local.dao.SetEntryDao
import com.fitquest.app.data.local.dao.WorkoutDao
import com.fitquest.app.data.local.dao.WorkoutExerciseDao
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity

/** The offline-first store. See: Google (2026) Save data using Room. https://developer.android.com/training/data-storage/room */
@Database(
    entities = [WorkoutEntity::class, WorkoutExerciseEntity::class, SetEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FitQuestDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        /** Adds foreign key indexes without touching stored workouts. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_exercises_workoutLocalId` " +
                        "ON `workout_exercises` (`workoutLocalId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_set_entries_workoutExerciseLocalId` " +
                        "ON `set_entries` (`workoutExerciseLocalId`)"
                )
            }
        }

        @Volatile private var INSTANCE: FitQuestDatabase? = null

        fun getInstance(context: Context): FitQuestDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitQuestDatabase::class.java,
                    "fitquest.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
