package com.fitquest.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.fitquest.app.viewmodel.AuthViewModel
import com.fitquest.app.viewmodel.BadgesViewModel
import com.fitquest.app.viewmodel.HomeViewModel
import com.fitquest.app.viewmodel.ProgressViewModel
import com.fitquest.app.viewmodel.SettingsViewModel
import com.fitquest.app.viewmodel.WorkoutDetailViewModel
import com.fitquest.app.viewmodel.WorkoutViewModel

/** Builds ViewModels with their dependencies from [AppContainer]. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(container.authRepository, container.googleSignInManager, container.profileRepository) as T

            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.workoutRepository, container.profileRepository) as T

            modelClass.isAssignableFrom(WorkoutViewModel::class.java) ->
                WorkoutViewModel(container.workoutRepository) as T

            modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java) ->
                WorkoutDetailViewModel(container.workoutRepository) as T

            modelClass.isAssignableFrom(BadgesViewModel::class.java) ->
                BadgesViewModel(container.workoutRepository) as T

            modelClass.isAssignableFrom(ProgressViewModel::class.java) ->
                ProgressViewModel(container.workoutRepository, container.photoRepository) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    container.preferencesRepository,
                    container.authRepository,
                    container.profileRepository,
                    container.workoutRepository
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
