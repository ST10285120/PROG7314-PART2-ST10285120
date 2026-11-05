package com.fitquest.app.data.repository

import android.util.Log
import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.RoutineDto
import com.fitquest.app.data.remote.dto.RoutinesPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Custom routines (FR2). Changes save on the phone first, then the whole list is sent
 * to the API. If that fails, a pending flag makes the next refresh upload it, so
 * routines work offline and still follow the user to a new phone.
 */
class RoutineRepository(
    private val api: FitQuestApi,
    private val localCache: LocalCache
) {

    private val _routines = MutableStateFlow(localCache.getRoutines().orEmpty())
    val routines: StateFlow<List<RoutineDto>> = _routines

    /** Uploads offline changes if there are any, otherwise takes the server's list. */
    suspend fun refresh() {
        _routines.value = localCache.getRoutines().orEmpty()
        if (localCache.hasPendingRoutines) {
            upload(_routines.value)
            return
        }
        try {
            val response = api.getRoutines()
            response.body()?.takeIf { response.isSuccessful }?.let { store(it.routines) }
        } catch (e: Exception) {
            Log.d(TAG, "Routine refresh skipped: ${e.message}")
        }
    }

    /** Adds a routine, or replaces the one with the same id. */
    suspend fun save(routine: RoutineDto) {
        val exists = _routines.value.any { it.id == routine.id }
        val updated = if (exists) {
            _routines.value.map { if (it.id == routine.id) routine else it }
        } else {
            _routines.value + routine
        }
        store(updated)
        upload(updated)
    }

    suspend fun delete(routineId: String) {
        val updated = _routines.value.filterNot { it.id == routineId }
        store(updated)
        upload(updated)
    }

    private fun store(routines: List<RoutineDto>) {
        localCache.saveRoutines(routines)
        _routines.value = routines
    }

    private suspend fun upload(routines: List<RoutineDto>) {
        val uploaded = try {
            api.saveRoutines(RoutinesPayload(routines)).isSuccessful
        } catch (e: Exception) {
            Log.d(TAG, "Routine upload deferred: ${e.message}")
            false
        }
        localCache.hasPendingRoutines = !uploaded
    }

    private companion object {
        const val TAG = "RoutineRepository"
    }
}
