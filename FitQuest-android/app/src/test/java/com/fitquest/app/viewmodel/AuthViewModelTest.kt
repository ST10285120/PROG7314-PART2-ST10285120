package com.fitquest.app.viewmodel

import com.fitquest.app.auth.GoogleSignInManager
import com.fitquest.app.auth.SsoResult
import com.fitquest.app.data.remote.SessionEvents
import com.fitquest.app.data.remote.dto.UserDto
import com.fitquest.app.data.repository.AuthRepository
import com.fitquest.app.data.repository.AuthResult
import com.fitquest.app.data.repository.ProfileRepository
import com.fitquest.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var googleSignInManager: GoogleSignInManager
    private lateinit var profileRepository: ProfileRepository
    private val fakeActivityContext: android.content.Context = mock()

    private val testUser = UserDto("u1", "Test User", "test@example.com", 120, 1, 4)

    @Before
    fun setUp() {
        authRepository = mock()
        googleSignInManager = mock()
        profileRepository = mock()
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(null))
    }

    private fun createViewModel() = AuthViewModel(authRepository, googleSignInManager, profileRepository)

    @Test
    fun `initial state is SignedOut when there is no cached session`() {
        whenever(authRepository.isSignedIn()).thenReturn(false)

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value is AuthUiState.SignedOut)
    }

    @Test
    fun `initial state resolves to SignedIn when a cached session already exists`() {
        whenever(authRepository.isSignedIn()).thenReturn(true)

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value is AuthUiState.SignedIn)
    }

    @Test
    fun `signInWithGoogle moves to SignedIn with the returned user on success`() = runTest {
        whenever(authRepository.isSignedIn()).thenReturn(false)
        whenever(googleSignInManager.signIn(fakeActivityContext)).thenReturn(SsoResult.Success("google-id-token"))
        whenever(authRepository.signInWithGoogle("google-id-token")).thenReturn(AuthResult.Success(testUser))

        val viewModel = createViewModel()
        viewModel.signInWithGoogle(fakeActivityContext)

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.SignedIn)
        assertEquals(testUser, (state as AuthUiState.SignedIn).user)
    }

    @Test
    fun `signInWithGoogle moves to Error without touching the repository when Credential Manager fails`() = runTest {
        whenever(authRepository.isSignedIn()).thenReturn(false)
        whenever(googleSignInManager.signIn(fakeActivityContext)).thenReturn(SsoResult.Failure("Sign-in was cancelled"))

        val viewModel = createViewModel()
        viewModel.signInWithGoogle(fakeActivityContext)

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Sign-in was cancelled", (state as AuthUiState.Error).message)
        verify(authRepository, never()).signInWithGoogle(org.mockito.kotlin.any())
    }

    @Test
    fun `signInWithGoogle moves to Error when the backend rejects the token`() = runTest {
        whenever(authRepository.isSignedIn()).thenReturn(false)
        whenever(googleSignInManager.signIn(fakeActivityContext)).thenReturn(SsoResult.Success("token"))
        whenever(authRepository.signInWithGoogle("token")).thenReturn(AuthResult.Error("Sign-in failed (HTTP 401)"))

        val viewModel = createViewModel()
        viewModel.signInWithGoogle(fakeActivityContext)

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Sign-in failed (HTTP 401)", (state as AuthUiState.Error).message)
    }

    @Test
    fun `signInWithGitHubCode moves to SignedIn on success`() = runTest {
        whenever(authRepository.isSignedIn()).thenReturn(false)
        whenever(authRepository.signInWithGitHub("auth-code")).thenReturn(AuthResult.Success(testUser))

        val viewModel = createViewModel()
        viewModel.signInWithGitHubCode("auth-code")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.SignedIn)
        assertEquals(testUser, (state as AuthUiState.SignedIn).user)
    }

    @Test
    fun `a cached session opens with the last known profile`() {
        whenever(authRepository.isSignedIn()).thenReturn(true)
        whenever(profileRepository.profile).thenReturn(MutableStateFlow(testUser))

        val viewModel = createViewModel()

        assertEquals(testUser, (viewModel.uiState.value as AuthUiState.SignedIn).user)
    }

    @Test
    fun `a successful sign-in saves the profile`() = runTest {
        whenever(authRepository.isSignedIn()).thenReturn(false)
        whenever(authRepository.signInWithGitHub("code")).thenReturn(AuthResult.Success(testUser))

        createViewModel().signInWithGitHubCode("code")

        verify(profileRepository).setProfile(testUser)
    }

    @Test
    fun `an expired session returns the user to sign-in`() {
        whenever(authRepository.isSignedIn()).thenReturn(true)
        val viewModel = createViewModel()

        SessionEvents.notifyExpired()

        assertTrue(viewModel.uiState.value is AuthUiState.SessionExpired)
    }

    @Test
    fun `a GitHub callback with a bad state value shows an error`() {
        whenever(authRepository.isSignedIn()).thenReturn(false)
        val viewModel = createViewModel()

        viewModel.rejectGitHubCallback()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
    }

    @Test
    fun `signOut clears the session and returns to SignedOut`() {
        whenever(authRepository.isSignedIn()).thenReturn(true)
        val viewModel = createViewModel()

        viewModel.signOut()

        verify(authRepository).signOut()
        assertTrue(viewModel.uiState.value is AuthUiState.SignedOut)
    }
}
