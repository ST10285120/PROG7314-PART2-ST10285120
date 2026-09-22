package com.fitquest.app.viewmodel

import com.fitquest.app.data.remote.dto.PreferencesDto
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.PreferencesRepository
import com.fitquest.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var authRepository: AuthRepository

    private val defaultPrefs = PreferencesDto(units = "KG", theme = "SYSTEM", reminderTime = "18:00", restTimerDefaultSec = 90)

    @Before
    fun setUp() {
        preferencesRepository = mock()
        authRepository = mock()
    }

    private fun createViewModel(): SettingsViewModel {
        whenever(preferencesRepository.getPreferences()).thenReturn(Result.success(defaultPrefs))
        return SettingsViewModel(preferencesRepository, authRepository)
    }

    @Test
    fun `loads preferences from the repository on init`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(defaultPrefs, state.preferences)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `sets an error message when loading preferences fails`() = runTest {
        whenever(preferencesRepository.getPreferences()).thenReturn(Result.failure(Exception("offline")))

        val viewModel = SettingsViewModel(preferencesRepository, authRepository)

        val state = viewModel.uiState.value
        assertEquals("offline", state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `updatePreferences optimistically applies the change then saves it`() = runTest {
        val viewModel = createViewModel()
        val saved = defaultPrefs.copy(units = "LB")
        whenever(preferencesRepository.updatePreferences(any())).thenReturn(Result.success(saved))

        viewModel.updatePreferences { it.copy(units = "LB") }

        val state = viewModel.uiState.value
        assertEquals("LB", state.preferences.units)
        assertEquals(false, state.isSaving)
        verify(preferencesRepository).updatePreferences(defaultPrefs.copy(units = "LB"))
    }

    @Test
    fun `updatePreferences sets an error message when saving fails`() = runTest {
        val viewModel = createViewModel()
        whenever(preferencesRepository.updatePreferences(any())).thenReturn(Result.failure(Exception("save failed")))

        viewModel.updatePreferences { it.copy(theme = "DARK") }

        val state = viewModel.uiState.value
        assertEquals("save failed", state.errorMessage)
        assertEquals(false, state.isSaving)
    }

    @Test
    fun `signOut calls the auth repository and flags signedOut`() = runTest {
        val viewModel = createViewModel()

        viewModel.signOut()

        verify(authRepository).signOut()
        assertTrue(viewModel.uiState.value.signedOut)
    }
}
