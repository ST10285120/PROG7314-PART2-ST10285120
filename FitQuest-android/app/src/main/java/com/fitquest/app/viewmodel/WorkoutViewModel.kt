package com.fitquest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.local.entity.SetEntryEntity
import com.fitquest.app.data.local.entity.WorkoutExerciseEntity
import com.fitquest.app.data.remote.dto.ExerciseDto
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import com.fitquest.app.data.repository.CompletionResult
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutUiState(
    val workoutLocalId: String? = null,
    val workoutStartedAt: Long? = null,
    val exerciseLibrary: List<ExerciseDto> = emptyList(),
    val currentExerciseLocalId: String? = null,
    val isLoadingLibrary: Boolean = false,
    val completion: CompletionResult? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val workoutRepository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState

    private val workoutExercisesFlow = MutableStateFlow<String?>(null)
    val exercisesInWorkout: StateFlow<List<WorkoutExerciseEntity>> = workoutExercisesFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else workoutRepository.observeExercisesForWorkout(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val currentSetsFlow = MutableStateFlow<String?>(null)
    val setsForCurrentExercise: StateFlow<List<SetEntryEntity>> = currentSetsFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else workoutRepository.observeSetsForWorkoutExercise(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startWorkout(title: String) {
        viewModelScope.launch {
            val id = workoutRepository.startWorkout(title)
            _uiState.value = _uiState.value.copy(workoutLocalId = id, workoutStartedAt = System.currentTimeMillis())
            workoutExercisesFlow.value = id
            loadExerciseLibrary()
        }
    }
    fun startWorkoutWithSelection(title: String, exercises: List<ExerciseDto>) {
        viewModelScope.launch {
            val id = workoutRepository.startWorkoutWithExerciseSelection(title, exercises)
            _uiState.value = _uiState.value.copy(workoutLocalId = id, workoutStartedAt = System.currentTimeMillis())
            workoutExercisesFlow.value = id
            loadExerciseLibrary()
            jumpToFirstExercise(id)
        }
    }
    fun startSuggestedWorkout(title: String, exercises: List<SuggestedWorkoutExerciseDto>) {
        viewModelScope.launch {
            val id = workoutRepository.startWorkoutWithExercises(title, exercises)
            _uiState.value = _uiState.value.copy(workoutLocalId = id, workoutStartedAt = System.currentTimeMillis())
            workoutExercisesFlow.value = id
            loadExerciseLibrary()
            jumpToFirstExercise(id)
        }
    }

    private suspend fun jumpToFirstExercise(workoutLocalId: String) {
        val firstExercise = workoutRepository.observeExercisesForWorkout(workoutLocalId).first().firstOrNull()
        if (firstExercise != null) {
            _uiState.value = _uiState.value.copy(currentExerciseLocalId = firstExercise.localId)
            currentSetsFlow.value = firstExercise.localId
        }
    }

    fun loadExerciseLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLibrary = true)
            workoutRepository.getExerciseLibrary().fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(exerciseLibrary = list, isLoadingLibrary = false)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingLibrary = false,
                        errorMessage = err.message ?: "Couldn't load the exercise library"
                    )
                }
            )
        }
    }

    fun addExercise(exercise: ExerciseDto, orderIndex: Int) {
        val workoutId = _uiState.value.workoutLocalId ?: return
        viewModelScope.launch {
            val weId = workoutRepository.addExercise(workoutId, exercise._id, exercise.name, orderIndex)
            _uiState.value = _uiState.value.copy(currentExerciseLocalId = weId)
            currentSetsFlow.value = weId
        }
    }
    fun addExercises(exercises: List<ExerciseDto>, startingOrderIndex: Int) {
        val workoutId = _uiState.value.workoutLocalId ?: return
        viewModelScope.launch {
            var firstNewWeId: String? = null
            exercises.forEachIndexed { offset, exercise ->
                val weId = workoutRepository.addExercise(workoutId, exercise._id, exercise.name, startingOrderIndex + offset)
                if (offset == 0) firstNewWeId = weId
            }
            if (_uiState.value.currentExerciseLocalId == null && firstNewWeId != null) {
                _uiState.value = _uiState.value.copy(currentExerciseLocalId = firstNewWeId)
                currentSetsFlow.value = firstNewWeId
            }
        }
    }
    fun goToNextExercise() {
        val queue = exercisesInWorkout.value
        val currentIndex = queue.indexOfFirst { it.localId == _uiState.value.currentExerciseLocalId }
        if (currentIndex == -1 || currentIndex + 1 >= queue.size) return
        val next = queue[currentIndex + 1]
        _uiState.value = _uiState.value.copy(currentExerciseLocalId = next.localId)
        currentSetsFlow.value = next.localId
    }

    fun hasNextExercise(): Boolean {
        val queue = exercisesInWorkout.value
        val currentIndex = queue.indexOfFirst { it.localId == _uiState.value.currentExerciseLocalId }
        return currentIndex != -1 && currentIndex + 1 < queue.size
    }

    fun logSet(exerciseId: String, setNumber: Int, reps: Int, weightKg: Double, rpe: Int?) {
        val weId = _uiState.value.currentExerciseLocalId ?: return
        viewModelScope.launch {
            workoutRepository.logSet(weId, exerciseId, setNumber, reps, weightKg, rpe)
        }
    }

    fun finishWorkout() {
        val workoutId = _uiState.value.workoutLocalId ?: return
        viewModelScope.launch {
            val result = workoutRepository.completeWorkout(workoutId)
            _uiState.value = _uiState.value.copy(completion = result)
        }
    }

    fun resetForNewWorkout() {
        _uiState.value = WorkoutUiState()
        workoutExercisesFlow.value = null
        currentSetsFlow.value = null
    }
}
