package com.fitquest.app.viewmodel

import com.fitquest.app.data.remote.dto.ProgressPhotoDto
import com.fitquest.app.data.remote.dto.ProgressResponse
import com.fitquest.app.data.repository.PhotoRepository
import com.fitquest.app.data.repository.WorkoutRepository
import com.fitquest.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var photoRepository: PhotoRepository

    private val progress = ProgressResponse(
        volumeByWeek = mapOf("2026-W10" to 1000.0),
        prsByExercise = mapOf("Squat" to 100.0),
        muscleGroupSplit = mapOf("LEGS" to 1000.0)
    )
    private val photo = ProgressPhotoDto("p1", "api/users/me/photos/p1/content", "2026-03-01T08:00:00.000Z")

    @Before
    fun setUp() {
        workoutRepository = mock()
        photoRepository = mock()
        whenever(workoutRepository.observeRecentCompleted()).thenReturn(flowOf(emptyList()))
        whenever(workoutRepository.observePersonalRecords()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `shows server progress when the API is reachable`() = runTest {
        whenever(workoutRepository.getProgress()).thenReturn(Result.success(progress))
        whenever(photoRepository.listPhotos()).thenReturn(Result.success(listOf(photo)))

        val state = ProgressViewModel(workoutRepository, photoRepository).uiState.value

        assertEquals(progress, state.progress)
        assertEquals(listOf(photo), state.photos)
        assertFalse(state.isOfflineData)
    }

    @Test
    fun `flags offline data when the API cannot be reached`() = runTest {
        whenever(workoutRepository.getProgress()).thenReturn(Result.failure(Exception("offline")))
        whenever(photoRepository.listPhotos()).thenReturn(Result.failure(Exception("offline")))

        val state = ProgressViewModel(workoutRepository, photoRepository).uiState.value

        assertTrue(state.isOfflineData)
        assertFalse(state.isLoading)
    }

    @Test
    fun `an uploaded photo goes to the front of the list`() = runTest {
        whenever(workoutRepository.getProgress()).thenReturn(Result.success(progress))
        whenever(photoRepository.listPhotos()).thenReturn(Result.success(emptyList()))
        whenever(photoRepository.uploadPhoto(any())).thenReturn(Result.success(photo))
        val viewModel = ProgressViewModel(workoutRepository, photoRepository)

        viewModel.uploadPhoto(byteArrayOf(1))

        assertEquals(listOf(photo), viewModel.uiState.value.photos)
        assertFalse(viewModel.uiState.value.isUploadingPhoto)
    }

    @Test
    fun `a failed upload shows an error and stops the spinner`() = runTest {
        whenever(workoutRepository.getProgress()).thenReturn(Result.success(progress))
        whenever(photoRepository.listPhotos()).thenReturn(Result.success(emptyList()))
        whenever(photoRepository.uploadPhoto(any())).thenReturn(Result.failure(Exception("offline")))
        val viewModel = ProgressViewModel(workoutRepository, photoRepository)

        viewModel.uploadPhoto(byteArrayOf(1))

        assertTrue(viewModel.uiState.value.photoError)
        assertFalse(viewModel.uiState.value.isUploadingPhoto)
    }

    @Test
    fun `deleting a photo removes it from the list`() = runTest {
        whenever(workoutRepository.getProgress()).thenReturn(Result.success(progress))
        whenever(photoRepository.listPhotos()).thenReturn(Result.success(listOf(photo)))
        whenever(photoRepository.deletePhoto("p1")).thenReturn(Result.success(Unit))
        val viewModel = ProgressViewModel(workoutRepository, photoRepository)

        viewModel.deletePhoto("p1")

        assertTrue(viewModel.uiState.value.photos.isEmpty())
    }
}
