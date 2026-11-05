package com.fitquest.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.data.remote.dto.RoutineDto
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import com.fitquest.app.data.repository.RoutineRepository
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Creates, edits, deletes and lists custom routines (FR2). */
class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val routines: StateFlow<List<RoutineDto>> = routineRepository.routines

    private val _exerciseLibrary = MutableStateFlow<List<ExerciseDto>>(emptyList())
    val exerciseLibrary: StateFlow<List<ExerciseDto>> = _exerciseLibrary

    fun load() {
        viewModelScope.launch {
            routineRepository.refresh()
            workoutRepository.getExerciseLibrary().onSuccess { _exerciseLibrary.value = it }
        }
    }

    /** Saves a new routine when routineId is null, otherwise updates that routine. */
    fun saveRoutine(routineId: String?, name: String, exercises: List<ExerciseDto>) {
        val routine = RoutineDto(
            id = routineId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            exercises = exercises.map { SuggestedWorkoutExerciseDto(it._id, it.name, it.muscleGroup) }
        )
        viewModelScope.launch {
            routineRepository.save(routine)
            Log.d(TAG, "Routine saved with ${exercises.size} exercise(s)")
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch { routineRepository.delete(routineId) }
    }

    private companion object {
        const val TAG = "RoutinesViewModel"
    }
}
