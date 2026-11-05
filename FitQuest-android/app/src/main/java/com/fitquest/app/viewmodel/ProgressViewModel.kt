package com.fitquest.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitquest.app.data.local.dao.PersonalRecordRow
import com.fitquest.app.data.local.entity.WorkoutEntity
import com.fitquest.app.data.remote.dto.ProgressPhotoDto
import com.fitquest.app.data.remote.dto.ProgressResponse
import com.fitquest.app.data.repository.PhotoRepository
import com.fitquest.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val progress: ProgressResponse? = null,
    val isLoading: Boolean = true,
    val isOfflineData: Boolean = false,
    val photos: List<ProgressPhotoDto> = emptyList(),
    val isUploadingPhoto: Boolean = false,
    val photoError: Boolean = false
)

/**
 * Prefers the API's progress data, which covers every device. If it is unreachable,
 * the screen shows the history stored in Room and says so.
 */
class ProgressViewModel(
    private val workoutRepository: WorkoutRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState

    val recentWorkouts: StateFlow<List<WorkoutEntity>> = workoutRepository
        .observeRecentCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localRecords: StateFlow<List<PersonalRecordRow>> = workoutRepository
        .observePersonalRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val progress = workoutRepository.getProgress()
            _uiState.value = _uiState.value.copy(
                progress = progress.getOrNull() ?: _uiState.value.progress ?: workoutRepository.getCachedProgress(),
                isOfflineData = progress.isFailure,
                isLoading = false
            )
            photoRepository.listPhotos().onSuccess { photos ->
                _uiState.value = _uiState.value.copy(photos = photos)
            }
        }
    }

    fun uploadPhoto(jpegBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingPhoto = true, photoError = false)
            photoRepository.uploadPhoto(jpegBytes).fold(
                onSuccess = { photo ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingPhoto = false,
                        photos = listOf(photo) + _uiState.value.photos
                    )
                },
                onFailure = { err ->
                    Log.w(TAG, "Photo upload failed: ${err.message}")
                    reportPhotoError()
                }
            )
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photoId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        photos = _uiState.value.photos.filterNot { it.id == photoId }
                    )
                },
                onFailure = { reportPhotoError() }
            )
        }
    }

    fun reportPhotoError() {
        _uiState.value = _uiState.value.copy(isUploadingPhoto = false, photoError = true)
    }

    private companion object {
        const val TAG = "ProgressViewModel"
    }
}
