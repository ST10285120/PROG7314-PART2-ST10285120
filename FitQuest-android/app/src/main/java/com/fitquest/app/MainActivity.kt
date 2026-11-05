package com.fitquest.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitquest.app.auth.BiometricAuthenticator
import com.fitquest.app.di.AppContainer
import com.fitquest.app.di.ViewModelFactory
import com.fitquest.app.notifications.PushTokenRegistrar
import com.fitquest.app.notifications.ReminderScheduler
import com.fitquest.app.ui.components.OfflineBanner
import com.fitquest.app.ui.navigation.Destination
import com.fitquest.app.ui.screens.*
import com.fitquest.app.ui.theme.FitQuestTheme
import com.fitquest.app.util.ImageCompressor
import com.fitquest.app.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The single Activity. It extends AppCompatActivity because BiometricPrompt needs a
 * FragmentActivity and the in-app language switch relies on AppCompat.
 */
class MainActivity : AppCompatActivity() {

    private val container by lazy { (application as FitQuestApp).container }
    private val factory by lazy { ViewModelFactory(container) }

    private val authViewModel: AuthViewModel by viewModels { factory }
    private var pendingGitHubCode by mutableStateOf<String?>(null)

    /** True until the user passes the biometric prompt in this session. */
    private var isLocked by mutableStateOf(true)
    private var unlockFailed by mutableStateOf(false)
    private var biometricEnabled by mutableStateOf(true)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        // A language change recreates this Activity, which must not lock the user out.
        isLocked = savedInstanceState?.getBoolean(KEY_LOCKED, true) ?: true
        biometricEnabled = container.localCache.biometricEnabled
        handleIntent(intent)

        setContent {
            val mustUnlock = isLocked && biometricEnabled && BiometricAuthenticator.isAvailable(this)
            FitQuestRoot(
                factory = factory,
                container = container,
                authViewModel = authViewModel,
                pendingGitHubCode = pendingGitHubCode,
                onGitHubCodeConsumed = { pendingGitHubCode = null },
                mustUnlock = mustUnlock,
                unlockFailed = unlockFailed,
                onRequestUnlock = ::promptForUnlock,
                onSignedOut = { isLocked = true },
                biometricEnabled = biometricEnabled,
                onBiometricChange = ::changeBiometricSetting
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LOCKED, isLocked)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS) {
            Log.i(TAG, "Relocking after time in the background")
            isLocked = true
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun promptForUnlock() {
        unlockFailed = false
        BiometricAuthenticator.authenticate(
            activity = this,
            title = getString(R.string.unlock_title),
            subtitle = getString(R.string.unlock_subtitle),
            onSuccess = { isLocked = false },
            onFailure = { unlockFailed = true }
        )
    }

    /** Turning the lock off needs verification, so someone holding an open phone cannot do it. */
    private fun changeBiometricSetting(enabled: Boolean) {
        if (enabled) {
            saveBiometricSetting(true)
            return
        }
        BiometricAuthenticator.authenticate(
            activity = this,
            title = getString(R.string.biometric_lock),
            subtitle = getString(R.string.unlock_subtitle),
            onSuccess = { saveBiometricSetting(false) },
            onFailure = { Log.d(TAG, "Biometric setting left unchanged") }
        )
    }

    private fun saveBiometricSetting(enabled: Boolean) {
        container.localCache.biometricEnabled = enabled
        biometricEnabled = enabled
        isLocked = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Receives the GitHub redirect. The code is only used if the state value matches. */
    private fun handleIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == BuildConfig.APPLICATION_ID && data.host == "oauth-callback") {
            val code = data.getQueryParameter("code") ?: return
            if (container.gitHubOAuthManager.isValidState(data.getQueryParameter("state"))) {
                pendingGitHubCode = code
            } else {
                authViewModel.rejectGitHubCallback()
            }
            // Stops a rotation from processing the same redirect again.
            setIntent(Intent(this, MainActivity::class.java))
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val RELOCK_AFTER_MS = 5 * 60 * 1000L
        const val KEY_LOCKED = "is_locked"
    }
}

/** Chooses between sign-in, the biometric lock and the main app. */
@Composable
fun FitQuestRoot(
    factory: ViewModelFactory,
    container: AppContainer,
    authViewModel: AuthViewModel,
    pendingGitHubCode: String?,
    onGitHubCodeConsumed: () -> Unit,
    mustUnlock: Boolean,
    unlockFailed: Boolean,
    onRequestUnlock: () -> Unit,
    onSignedOut: () -> Unit,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingGitHubCode) {
        if (pendingGitHubCode != null) {
            authViewModel.signInWithGitHubCode(pendingGitHubCode)
            onGitHubCodeConsumed()
        }
    }
    LaunchedEffect(authState) {
        if (authState !is AuthUiState.SignedIn) onSignedOut()
    }

    when (authState) {
        is AuthUiState.Loading, is AuthUiState.SignedOut, is AuthUiState.SessionExpired, is AuthUiState.Error -> {
            FitQuestTheme {
                val context = LocalContext.current
                val noBrowserMessage = "No browser available"
                SsoSignInScreen(
                    uiState = authState,
                    onSignInWithGoogle = { authViewModel.signInWithGoogle(context) },
                    onSignInWithGitHub = {
                        if (!container.gitHubOAuthManager.launch(context)) {
                            authViewModel.reportSignInError(noBrowserMessage)
                        }
                    }
                )
            }
        }
        is AuthUiState.SignedIn -> {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val useDark = when (settingsState.preferences.theme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            FitQuestTheme(useDarkTheme = useDark) {
                // The lock covers the app instead of replacing it. Removing the app would
                // reset navigation and throw the user out of a workout in progress.
                Box(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(
                        factory = factory,
                        container = container,
                        settingsViewModel = settingsViewModel,
                        settingsState = settingsState,
                        isLocked = mustUnlock,
                        onSignedOut = { authViewModel.signOut() },
                        biometricEnabled = biometricEnabled,
                        onBiometricChange = onBiometricChange
                    )
                    if (mustUnlock) {
                        BiometricLockScreen(
                            showError = unlockFailed,
                            onUnlock = onRequestUnlock,
                            onSignOut = { authViewModel.signOut() }
                        )
                    }
                }
            }
        }
    }
}

private data class TopBarSpec(val titleRes: Int, val showBack: Boolean)

private fun topBarFor(route: String?): TopBarSpec = when {
    route == Destination.ActiveWorkout.route -> TopBarSpec(R.string.title_active_workout, showBack = false)
    route == Destination.WorkoutSummary.route -> TopBarSpec(R.string.title_summary, showBack = false)
    route == Destination.Progress.route -> TopBarSpec(R.string.nav_progress, showBack = false)
    route == Destination.Badges.route -> TopBarSpec(R.string.nav_badges, showBack = false)
    route == Destination.Settings.route -> TopBarSpec(R.string.nav_settings, showBack = false)
    route == Destination.Routines.route -> TopBarSpec(R.string.my_routines, showBack = true)
    route?.startsWith("workout_detail/") == true -> TopBarSpec(R.string.title_workout_detail, showBack = true)
    else -> TopBarSpec(R.string.app_name, showBack = false)
}

/** Moves between bottom tabs without stacking them, keeping each tab's state. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    factory: ViewModelFactory,
    container: AppContainer,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    isLocked: Boolean,
    onSignedOut: () -> Unit,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val workoutViewModel: WorkoutViewModel = viewModel(factory = factory)
    val scope = rememberCoroutineScope()

    val profile by homeViewModel.profile.collectAsStateWithLifecycle()
    val units = settingsState.preferences.units
    val isOnline by remember { container.connectivityObserver.observe() }
        .collectAsStateWithLifecycle(initialValue = true)

    // On reconnecting, push queued workouts and pull fresh data straight away.
    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(isOnline) {
        if (isOnline && wasOffline) {
            Log.i("MainAppScaffold", "Back online; refreshing")
            homeViewModel.refresh()
            settingsViewModel.load()
        }
        wasOffline = !isOnline
    }

    LaunchedEffect(settingsState.signedOut) {
        if (settingsState.signedOut) {
            settingsViewModel.consumeSignedOut()
            homeViewModel.markSessionEnded()
            onSignedOut()
        }
    }
    // These ViewModels belong to the Activity, so reload them for each signed-in session.
    LaunchedEffect(Unit) {
        homeViewModel.ensureLoaded()
        settingsViewModel.load()
    }
    LaunchedEffect(settingsState.preferences.reminderTime) {
        ReminderScheduler.schedule(context, settingsState.preferences.reminderTime)
    }
    LaunchedEffect(Unit) {
        PushTokenRegistrar.register(scope, container.profileRepository)
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> Log.d("MainAppScaffold", "Notification permission granted=$granted") }
    LaunchedEffect(isLocked) {
        if (!isLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { ImageCompressor.toJpeg(context, uri) }
                if (bytes != null) settingsViewModel.uploadAvatar(bytes) else settingsViewModel.reportAvatarError()
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = setOf(
        Destination.Home.route, Destination.Progress.route,
        Destination.Badges.route, Destination.Settings.route
    )
    val topBar = topBarFor(currentRoute)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(topBar.titleRes), fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        if (topBar.showBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                if (!isOnline) OfflineBanner()
            }
        },
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    TabItem(navController, currentRoute, Destination.Home.route, Icons.Filled.Home, R.string.nav_home)
                    TabItem(navController, currentRoute, Destination.Progress.route, Icons.Filled.BarChart, R.string.nav_progress)
                    TabItem(navController, currentRoute, Destination.Badges.route, Icons.Filled.EmojiEvents, R.string.nav_badges)
                    TabItem(navController, currentRoute, Destination.Settings.route, Icons.Filled.Settings, R.string.nav_settings)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding).imePadding()
        ) {
            composable(Destination.Home.route) {
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val recent by homeViewModel.recentWorkouts.collectAsStateWithLifecycle()
                val pendingXp by homeViewModel.pendingXp.collectAsStateWithLifecycle()
                val activeWorkout by homeViewModel.activeWorkout.collectAsStateWithLifecycle()
                val defaultTitle = stringResource(R.string.default_workout_title)
                val suggestedTitle = stringResource(R.string.suggested_workout_title)
                HomeScreen(
                    uiState = uiState,
                    profile = profile,
                    pendingXp = pendingXp,
                    activeWorkout = activeWorkout,
                    units = units,
                    recentWorkouts = recent,
                    onStartWorkout = {
                        workoutViewModel.resetForNewWorkout()
                        workoutViewModel.startWorkout(defaultTitle)
                        navController.navigate(Destination.ActiveWorkout.route)
                    },
                    onOpenRoutines = { navController.navigate(Destination.Routines.route) },
                    onResumeWorkout = { workout ->
                        workoutViewModel.resumeWorkout(workout)
                        navController.navigate(Destination.ActiveWorkout.route)
                    },
                    onClickSuggestedWorkout = {
                        workoutViewModel.resetForNewWorkout()
                        workoutViewModel.startSuggestedWorkout(suggestedTitle, uiState.suggestedExercises)
                        navController.navigate(Destination.ActiveWorkout.route)
                    },
                    onClickStreak = { navController.navigateToTab(Destination.Badges.route) },
                    onClickWorkout = { workoutLocalId ->
                        navController.navigate(Destination.WorkoutDetail.createRoute(workoutLocalId))
                    }
                )
            }

            composable(Destination.Routines.route) {
                val routinesViewModel: RoutinesViewModel = viewModel(factory = factory)
                val routines by routinesViewModel.routines.collectAsStateWithLifecycle()
                val library by routinesViewModel.exerciseLibrary.collectAsStateWithLifecycle()
                val activeWorkout by homeViewModel.activeWorkout.collectAsStateWithLifecycle()
                LaunchedEffect(isOnline) { routinesViewModel.load() }
                RoutinesScreen(
                    routines = routines,
                    exerciseLibrary = library,
                    hasActiveWorkout = activeWorkout != null,
                    onStartRoutine = { routine ->
                        workoutViewModel.resetForNewWorkout()
                        workoutViewModel.startSuggestedWorkout(routine.name, routine.exercises)
                        navController.navigate(Destination.ActiveWorkout.route) {
                            popUpTo(Destination.Home.route)
                        }
                    },
                    onSaveRoutine = routinesViewModel::saveRoutine,
                    onDeleteRoutine = routinesViewModel::deleteRoutine
                )
            }

            composable(Destination.ActiveWorkout.route) {
                val uiState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                val exercisesInWorkout by workoutViewModel.exercisesInWorkout.collectAsStateWithLifecycle()
                val setsForCurrent by workoutViewModel.setsForCurrentExercise.collectAsStateWithLifecycle()
                val totalSets by workoutViewModel.totalSets.collectAsStateWithLifecycle()

                // The Part 1 design makes this flow forward-only, so a stray back press
                // cannot lose logging context. Finish or Cancel are the ways out.
                BackHandler { Log.d("MainAppScaffold", "Back ignored during a workout") }

                LaunchedEffect(Unit) { workoutViewModel.loadExerciseLibrary() }

                val currentIndex = exercisesInWorkout.indexOfFirst { it.localId == uiState.currentExerciseLocalId }
                val currentExercise = exercisesInWorkout.getOrNull(currentIndex)
                val nextExercise = if (currentIndex >= 0) exercisesInWorkout.getOrNull(currentIndex + 1) else null

                ActiveWorkoutScreen(
                    exerciseLibrary = uiState.exerciseLibrary,
                    currentExercise = currentExercise,
                    nextExerciseName = nextExercise?.exerciseName,
                    setsForCurrentExercise = setsForCurrent,
                    totalSets = totalSets,
                    workoutStartedAt = uiState.workoutStartedAt,
                    units = units,
                    restTimerDefaultSec = settingsState.preferences.restTimerDefaultSec,
                    onPickExercises = { selected ->
                        workoutViewModel.addExercises(selected, exercisesInWorkout.size)
                    },
                    onLogSet = { reps, weightKg, rpe ->
                        val exerciseId = currentExercise?.exerciseId ?: return@ActiveWorkoutScreen
                        // Deleting a set can leave a gap, so the size is not a safe next number.
                        val nextSetNumber = (setsForCurrent.maxOfOrNull { it.setNumber } ?: 0) + 1
                        workoutViewModel.logSet(exerciseId, nextSetNumber, reps, weightKg, rpe)
                    },
                    onDeleteSet = { set -> workoutViewModel.deleteSet(set) },
                    onNextExercise = { workoutViewModel.goToNextExercise() },
                    onFinishWorkout = {
                        workoutViewModel.finishWorkout()
                        navController.navigate(Destination.WorkoutSummary.route) {
                            popUpTo(Destination.Home.route)
                        }
                    },
                    onCancelWorkout = {
                        workoutViewModel.discardWorkout()
                        navController.popBackStack()
                    }
                )
            }

            composable(Destination.WorkoutSummary.route) {
                val uiState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                val finishSummary = {
                    homeViewModel.loadSuggestion()
                    homeViewModel.refreshProfile()
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Home.route) { inclusive = true }
                    }
                }
                BackHandler(onBack = finishSummary)
                WorkoutSummaryScreen(completion = uiState.completion, onDone = finishSummary)
            }

            composable(Destination.Progress.route) {
                val progressViewModel: ProgressViewModel = viewModel(factory = factory)
                val progressState by progressViewModel.uiState.collectAsStateWithLifecycle()
                val recent by progressViewModel.recentWorkouts.collectAsStateWithLifecycle()
                val records by progressViewModel.localRecords.collectAsStateWithLifecycle()
                // Runs each time the tab opens, so a finished workout appears straight away.
                LaunchedEffect(isOnline) { if (isOnline) progressViewModel.load() }
                val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { ImageCompressor.toJpeg(context, uri) }
                            if (bytes != null) progressViewModel.uploadPhoto(bytes) else progressViewModel.reportPhotoError()
                        }
                    }
                }
                ProgressScreen(
                    uiState = progressState,
                    recentWorkouts = recent,
                    localRecords = records,
                    units = units,
                    onClickWorkout = { workoutLocalId ->
                        navController.navigate(Destination.WorkoutDetail.createRoute(workoutLocalId))
                    },
                    onAddPhoto = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onDeletePhoto = progressViewModel::deletePhoto
                )
            }

            composable(
                route = Destination.WorkoutDetail.route,
                arguments = listOf(navArgument(Destination.WorkoutDetail.ARG_WORKOUT_LOCAL_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutLocalId = backStackEntry.arguments?.getString(Destination.WorkoutDetail.ARG_WORKOUT_LOCAL_ID)
                val detailViewModel: WorkoutDetailViewModel = viewModel(factory = factory)
                LaunchedEffect(workoutLocalId) {
                    if (workoutLocalId != null) detailViewModel.load(workoutLocalId)
                }
                val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
                WorkoutDetailScreen(uiState = detailState, units = units)
            }

            composable(Destination.Badges.route) {
                val badgesViewModel: BadgesViewModel = viewModel(factory = factory)
                val badgesState by badgesViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(isOnline) { if (isOnline) badgesViewModel.load() }
                BadgesScreen(
                    uiState = badgesState,
                    currentStreak = profile?.currentStreak ?: 0,
                    longestStreak = profile?.longestStreak ?: 0
                )
            }

            composable(Destination.Settings.route) {
                val language = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                SettingsScreen(
                    uiState = settingsState,
                    profile = profile,
                    language = language.substringBefore('-'),
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = BiometricAuthenticator.isAvailable(context),
                    onUnitsChange = { selected -> settingsViewModel.updatePreferences { it.copy(units = selected) } },
                    onThemeChange = { theme -> settingsViewModel.updatePreferences { it.copy(theme = theme) } },
                    onLanguageChange = { tag ->
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    },
                    onBiometricChange = onBiometricChange,
                    onSave = { reminderTime, restTimerSec ->
                        settingsViewModel.saveReminderAndRestTimer(reminderTime, restTimerSec)
                    },
                    onChangeAvatar = {
                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTestNotification = { settingsViewModel.sendTestNotification() },
                    onLogOut = { settingsViewModel.signOut() },
                    onDeleteAccount = { settingsViewModel.deleteAccount() }
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    navController: NavHostController,
    currentRoute: String?,
    route: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int
) {
    val label = stringResource(labelRes)
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = { navController.navigateToTab(route) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, maxLines = 1) }
    )
}
