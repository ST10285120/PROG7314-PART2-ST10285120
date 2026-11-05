package com.fitquest.app.data.repository

import com.fitquest.app.data.local.LocalCache
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.UserDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class ProfileRepositoryTest {

    private lateinit var api: FitQuestApi
    private lateinit var cache: LocalCache
    private val cached = UserDto("u1", "Sam", "sam@example.com", 100, 1, 2)
    private val fresh = cached.copy(xpTotal = 300, level = 2, currentStreak = 3)

    @Before
    fun setUp() {
        api = mock()
        cache = mock()
        whenever(cache.getProfile()).thenReturn(cached)
    }

    @Test
    fun `starts from the cached profile`() {
        assertEquals(cached, ProfileRepository(api, cache).profile.value)
    }

    @Test
    fun `refresh replaces the profile and updates the cache`() = runTest {
        whenever(api.getProfile()).thenReturn(Response.success(fresh))
        val repository = ProfileRepository(api, cache)

        repository.refresh()

        assertEquals(fresh, repository.profile.value)
        verify(cache).saveProfile(fresh)
    }

    @Test
    fun `refresh keeps the cached profile when offline`() = runTest {
        whenever(api.getProfile()).thenThrow(RuntimeException("offline"))
        val repository = ProfileRepository(api, cache)

        repository.refresh()

        assertEquals(cached, repository.profile.value)
    }

    @Test
    fun `avatar upload reports failure rather than throwing`() = runTest {
        whenever(api.uploadAvatar(any())).thenThrow(RuntimeException("offline"))

        val result = ProfileRepository(api, cache).uploadAvatar(byteArrayOf(1))

        assertTrue(result.isFailure)
    }
}
