package com.fitquest.app.data.repository

import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.RoutineDto
import com.fitquest.app.data.remote.dto.RoutinesPayload
import com.fitquest.app.data.remote.dto.SuggestedWorkoutExerciseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class RoutineRepositoryTest {

    private lateinit var api: FitQuestApi
    private lateinit var cache: LocalCache
    private val routine = RoutineDto(
        "r1",
        "Push Day",
        listOf(SuggestedWorkoutExerciseDto("ex1", "Bench", "CHEST"))
    )

    @Before
    fun setUp() {
        api = mock()
        cache = mock()
        whenever(cache.getRoutines()).thenReturn(emptyList())
    }

    @Test
    fun `save keeps the routine locally and uploads it`() = runTest {
        whenever(api.saveRoutines(any())).thenReturn(Response.success(RoutinesPayload(listOf(routine))))
        val repository = RoutineRepository(api, cache)

        repository.save(routine)

        assertEquals(listOf(routine), repository.routines.value)
        verify(cache).hasPendingRoutines = false
    }

    @Test
    fun `an offline save is kept and marked as pending`() = runTest {
        whenever(api.saveRoutines(any())).thenThrow(RuntimeException("offline"))
        val repository = RoutineRepository(api, cache)

        repository.save(routine)

        assertEquals(listOf(routine), repository.routines.value)
        verify(cache).hasPendingRoutines = true
    }

    @Test
    fun `saving an existing id replaces it`() = runTest {
        whenever(api.saveRoutines(any())).thenReturn(Response.success(RoutinesPayload(emptyList())))
        val repository = RoutineRepository(api, cache)
        repository.save(routine)

        repository.save(routine.copy(name = "Upper Day"))

        assertEquals(listOf("Upper Day"), repository.routines.value.map { it.name })
    }

    @Test
    fun `delete removes the routine`() = runTest {
        whenever(api.saveRoutines(any())).thenReturn(Response.success(RoutinesPayload(emptyList())))
        val repository = RoutineRepository(api, cache)
        repository.save(routine)

        repository.delete("r1")

        assertTrue(repository.routines.value.isEmpty())
    }

    @Test
    fun `refresh uploads pending changes instead of overwriting them`() = runTest {
        whenever(cache.hasPendingRoutines).thenReturn(true)
        whenever(cache.getRoutines()).thenReturn(listOf(routine))
        whenever(api.saveRoutines(any())).thenReturn(Response.success(RoutinesPayload(listOf(routine))))
        val repository = RoutineRepository(api, cache)

        repository.refresh()

        verify(api).saveRoutines(RoutinesPayload(listOf(routine)))
        verify(api, never()).getRoutines()
    }

    @Test
    fun `refresh takes the server list when nothing is pending`() = runTest {
        whenever(api.getRoutines()).thenReturn(Response.success(RoutinesPayload(listOf(routine))))
        val repository = RoutineRepository(api, cache)

        repository.refresh()

        assertEquals(listOf(routine), repository.routines.value)
    }
}
