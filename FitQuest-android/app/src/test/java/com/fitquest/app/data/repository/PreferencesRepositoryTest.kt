package com.fitquest.app.data.repository

import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.PreferencesDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

class PreferencesRepositoryTest {

    private lateinit var api: FitQuestApi
    private lateinit var repository: PreferencesRepository

    private val defaultPrefs = PreferencesDto(units = "KG", theme = "SYSTEM", reminderTime = "18:00", restTimerDefaultSec = 90)

    @Before
    fun setUp() {
        api = mock()
        repository = PreferencesRepository(api)
    }

    @Test
    fun `getPreferences returns success with the body on 2xx`() = runTest {
        whenever(api.getPreferences()).thenReturn(Response.success(defaultPrefs))

        val result = repository.getPreferences()

        assertTrue(result.isSuccess)
        assertEquals(defaultPrefs, result.getOrNull())
    }

    @Test
    fun `getPreferences returns failure on non-2xx`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(api.getPreferences()).thenReturn(Response.error(500, errorBody))

        val result = repository.getPreferences()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getPreferences returns failure when the call throws`() = runTest {
        whenever(api.getPreferences()).thenThrow(RuntimeException("timeout"))

        val result = repository.getPreferences()

        assertTrue(result.isFailure)
        assertEquals("timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun `updatePreferences returns the saved preferences on success`() = runTest {
        val updated = defaultPrefs.copy(units = "LB", theme = "DARK")
        whenever(api.updatePreferences(any())).thenReturn(Response.success(updated))

        val result = repository.updatePreferences(updated)

        assertTrue(result.isSuccess)
        assertEquals("LB", result.getOrNull()?.units)
        assertEquals("DARK", result.getOrNull()?.theme)
    }

    @Test
    fun `updatePreferences returns failure on non-2xx`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(api.updatePreferences(any())).thenReturn(Response.error(400, errorBody))

        val result = repository.updatePreferences(defaultPrefs)

        assertTrue(result.isFailure)
    }
}
