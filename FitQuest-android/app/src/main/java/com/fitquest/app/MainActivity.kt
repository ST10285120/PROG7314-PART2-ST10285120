package com.fitquest.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitquest.app.auth.GitHubOAuthManager
import com.fitquest.app.di.ViewModelFactory
import com.fitquest.app.ui.navigation.Destination
import com.fitquest.app.ui.screens.*
import com.fitquest.app.ui.theme.FitQuestTheme
import com.fitquest.app.viewmodel.*

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as FitQuestApp).container }
    private val factory by lazy { ViewModelFactory(container) }

    private val authViewModel: AuthViewModel by viewModels { factory }
    private var pendingGitHubCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            FitQuestApp(
                factory = factory,
                authViewModel = authViewModel,
                pendingGitHubCode = pendingGitHubCode,
                onGitHubCodeConsumed = { pendingGitHubCode = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == BuildConfig.APPLICATION_ID || data.host == "oauth-callback") {
            val code = data.getQueryParameter("code")
            if (code != null) pendingGitHubCode = code
        }
    }
}

@Composable
fun FitQuestApp(
    factory: ViewModelFactory,
    authViewModel: AuthViewModel,
    pendingGitHubCode: String?,
    onGitHubCodeConsumed: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingGitHubCode) {
        if (pendingGitHubCode != null) {
            authViewModel.signInWithGitHubCode(pendingGitHubCode)
            onGitHubCodeConsumed()
        }
    }
    when (authState) {
        is AuthUiState.Loading -> {
            FitQuestTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        is AuthUiState.SignedOut, is AuthUiState.Error -> {
            FitQuestTheme {
                val context = LocalContext.current
                val gitHubOAuthManager = remember { GitHubOAuthManager(context) }
                SsoSignInScreen(
                    uiState = authState,
                    onSignInWithGoogle = { authViewModel.signInWithGoogle(context) },
                    onSignInWithGitHub = { gitHubOAuthManager.launch(gitHubOAuthManager.generateState()) },
                    gitHubOAuthManager = gitHubOAuthManager
                )
            }
        }
        is AuthUiState.SignedIn -> {
            val user = (authState as AuthUiState.SignedIn).user
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val useDark = when (settingsState.preferences.theme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            FitQuestTheme(useDarkTheme = useDark) {
                MainAppScaffold(
                    factory = factory,
                    settingsViewModel = settingsViewModel,
                    settingsState = settingsState,
                    onSignedOut = { authViewModel.signOut() },
                    initialUserXp = user.xpTotal
                )
            }
        }
    }
}

private data class TopBarSpec(val title: String, val showBack: Boolean)

private fun topBarFor(route: String?): TopBarSpec = when {
    route == Destination.Home.route -> TopBarSpec("FitQuest", showBack = false)
    route == Destination.ActiveWorkout.route -> TopBarSpec("Active Workout", showBack = true)
    route == Destination.WorkoutSummary.route -> TopBarSpec("Summary", showBack = false)
    route == Destination.Progress.route -> TopBarSpec("Progress", showBack = false)
    route == Destination.Badges.route -> TopBarSpec("Badges & Streak", showBack = false)
    route == Destination.Settings.route -> TopBarSpec("Settings", showBack = false)
    route?.startsWith("workout_detail/") == true -> TopBarSpec("Workout Detail", showBack = true)
    else -> TopBarSpec("FitQuest", showBack = false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    factory: ViewModelFactory,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onSignedOut: () -> Unit,
    initialUserXp: Int
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val workoutViewModel: WorkoutViewModel = viewModel(factory = factory)

    LaunchedEffect(settingsState.signedOut) {
        if (settingsState.signedOut) onSignedOut()
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
            TopAppBar(
                title = { Text(topBar.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (topBar.showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Destination.Home.route,
                        onClick = { navController.navigate(Destination.Home.route) { popUpTo(navController.graph.findStartDestination().id) } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destination.Progress.route,
                        onClick = { navController.navigate(Destination.Progress.route) },
                        icon = { Icon(Icons.Filled.BarChart, contentDescription = "Progress") },
                        label = { Text("Progress") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destination.Badges.route,
                        onClick = { navController.navigate(Destination.Badges.route) },
                        icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = "Badges") },
                        label = { Text("Badges") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destination.Settings.route,
                        onClick = { navController.navigate(Destination.Settings.route) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Home.route) {
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val recent by homeViewModel.recentWorkouts.collectAsStateWithLifecycle()
                val totalXp by homeViewModel.totalXp.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    recentWorkouts = recent,
                    currentStreak = recent.count(),
                    level = 1 + totalXp / 200,
                    xpTotal = totalXp,
                    onStartWorkout = {
                        workoutViewModel.resetForNewWorkout()
                        workoutViewModel.startWorkout("Workout")
                        navController.navigate(Destination.ActiveWorkout.route)
                    },
                    onClickSuggestedWorkout = {
                        workoutViewModel.resetForNewWorkout()
                        workoutViewModel.startSuggestedWorkout(uiState.suggestedTitle, uiState.suggestedExercises)
                        navController.navigate(Destination.ActiveWorkout.route)
                    },
                    onClickStreak = { navController.navigate(Destination.Badges.route) },
                    onClickWorkout = { workoutLocalId ->
                        navController.navigate(Destination.WorkoutDetail.createRoute(workoutLocalId))
                    }
                )
            }

            composable(Destination.ActiveWorkout.route) {
                val uiState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                val exercisesInWorkout by workoutViewModel.exercisesInWorkout.collectAsStateWithLifecycle()
                val setsForCurrent by workoutViewModel.setsForCurrentExercise.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) { workoutViewModel.loadExerciseLibrary() }

                val currentExercise = exercisesInWorkout.find { it.localId == uiState.currentExerciseLocalId }

                ActiveWorkoutScreen(
                    exerciseLibrary = uiState.exerciseLibrary,
                    exercisesInWorkout = exercisesInWorkout,
                    currentExercise = currentExercise,
                    setsForCurrentExercise = setsForCurrent,
                    workoutStartedAt = uiState.workoutStartedAt,
                    hasNextExercise = workoutViewModel.hasNextExercise(),
                    restTimerDefaultSec = settingsState.preferences.restTimerDefaultSec,
                    onPickExercises = { selected ->
                        workoutViewModel.addExercises(selected, exercisesInWorkout.size)
                    },
                    onLogSet = { reps, weightKg, rpe ->
                        val exerciseId = currentExercise?.exerciseId ?: return@ActiveWorkoutScreen
                        val nextSetNumber = setsForCurrent.size + 1
                        workoutViewModel.logSet(exerciseId, nextSetNumber, reps, weightKg, rpe)
                    },
                    onNextExercise = { workoutViewModel.goToNextExercise() },
                    onFinishWorkout = {
                        workoutViewModel.finishWorkout()
                        navController.navigate(Destination.WorkoutSummary.route)
                    }
                )
            }

            composable(Destination.WorkoutSummary.route) {
                val uiState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                WorkoutSummaryScreen(
                    completion = uiState.completion,
                    onDone = {
                        homeViewModel.loadSuggestion()
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destination.Progress.route) {
                val recent by homeViewModel.recentWorkouts.collectAsStateWithLifecycle()
                val prs by homeViewModel.personalRecords.collectAsStateWithLifecycle()
                ProgressScreen(
                    recentWorkouts = recent,
                    personalRecords = prs,
                    onClickWorkout = { workoutLocalId ->
                        navController.navigate(Destination.WorkoutDetail.createRoute(workoutLocalId))
                    }
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
                WorkoutDetailScreen(uiState = detailState)
            }

            composable(Destination.Badges.route) {
                val badgesViewModel: BadgesViewModel = viewModel(factory = factory)
                val badgesState by badgesViewModel.uiState.collectAsStateWithLifecycle()
                val recent by homeViewModel.recentWorkouts.collectAsStateWithLifecycle()
                BadgesScreen(uiState = badgesState, currentStreak = recent.count())
            }

            composable(Destination.Settings.route) {
                SettingsScreen(
                    uiState = settingsState,
                    onUnitsChange = { units -> settingsViewModel.updatePreferences { it.copy(units = units) } },
                    onThemeChange = { theme -> settingsViewModel.updatePreferences { it.copy(theme = theme) } },
                    onSave = { reminderTime, restTimerSec -> settingsViewModel.saveReminderAndRestTimer(reminderTime, restTimerSec) },
                    onLogOut = { settingsViewModel.signOut() }
                )
            }
        }
    }
}
