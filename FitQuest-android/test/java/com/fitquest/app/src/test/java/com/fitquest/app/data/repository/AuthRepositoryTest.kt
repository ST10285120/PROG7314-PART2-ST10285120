package com.fitquest.app.data.repository

import com.fitquest.app.data.local.TokenStore
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.SsoRequest
import com.fitquest.app.data.remote.dto.SsoResponse
import com.fitquest.app.data.remote.dto.UserDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
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

class AuthRepositoryTest {

    private lateinit var api: FitQuestApi
    private lateinit var tokenStore: TokenStore
    private lateinit var repository: AuthRepository

    private val testUser = UserDto(
        id = "u1", username = "Test User", email = "test@example.com",
        xpTotal = 0, level = 1, currentStreak = 0
    )

    @Before
    fun setUp() {
        api = mock()
        tokenStore = mock()
        repository = AuthRepository(api, tokenStore)
    }

    @Test
    fun `isSignedIn delegates to token store`() {
        whenever(tokenStore.isSignedIn()).thenReturn(true)
        assertTrue(repository.isSignedIn())

        whenever(tokenStore.isSignedIn()).thenReturn(false)
        assertTrue(!repository.isSignedIn())
    }

    @Test
    fun `signInWithGoogle saves token and returns Success on 2xx response`() = runTest {
        whenever(api.ssoSignIn(any())).thenReturn(Response.success(SsoResponse("jwt-123", testUser)))

        val result = repository.signInWithGoogle("google-id-token")

        assertTrue(result is AuthResult.Success)
        assertEquals(testUser, (result as AuthResult.Success).user)
        verify(tokenStore).saveToken("jwt-123")
    }

    @Test
    fun `signInWithGoogle sends provider GOOGLE and the idToken, not a code`() = runTest {
        whenever(api.ssoSignIn(any())).thenReturn(Response.success(SsoResponse("jwt", testUser)))

        repository.signInWithGoogle("the-id-token")

        val captor = org.mockito.kotlin.argumentCaptor<SsoRequest>()
        verify(api).ssoSignIn(captor.capture())
        assertEquals("GOOGLE", captor.firstValue.provider)
        assertEquals("the-id-token", captor.firstValue.idToken)
        assertEquals(null, captor.firstValue.code)
    }

    @Test
    fun `signInWithGitHub sends provider GITHUB and the code, not an idToken`() = runTest {
        whenever(api.ssoSignIn(any())).thenReturn(Response.success(SsoResponse("jwt", testUser)))

        repository.signInWithGitHub("auth-code-123")

        val captor = org.mockito.kotlin.argumentCaptor<SsoRequest>()
        verify(api).ssoSignIn(captor.capture())
        assertEquals("GITHUB", captor.firstValue.provider)
        assertEquals("auth-code-123", captor.firstValue.code)
        assertEquals(null, captor.firstValue.idToken)
    }

    @Test
    fun `signIn returns Error and does not save a token on non-2xx response`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(api.ssoSignIn(any())).thenReturn(Response.error(401, errorBody))

        val result = repository.signInWithGoogle("bad-token")

        assertTrue(result is AuthResult.Error)
        assertTrue((result as AuthResult.Error).message.contains("401"))
        verify(tokenStore, never()).saveToken(any())
    }

    @Test
    fun `signIn returns Error when the network call throws`() = runTest {
        whenever(api.ssoSignIn(any())).thenThrow(RuntimeException("no connectivity"))

        val result = repository.signInWithGoogle("token")

        assertTrue(result is AuthResult.Error)
        assertEquals("no connectivity", (result as AuthResult.Error).message)
        verify(tokenStore, never()).saveToken(any())
    }

    @Test
    fun `signOut clears the cached token`() {
        repository.signOut()
        verify(tokenStore).clearToken()
    }
}
