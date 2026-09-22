package com.fitquest.app.data.remote

import com.fitquest.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface FitQuestApi {

    @POST("api/auth/sso")
    suspend fun ssoSignIn(@Body request: SsoRequest): Response<SsoResponse>

    @GET("api/exercises")
    suspend fun listExercises(
        @Query("muscleGroup") muscleGroup: String? = null,
        @Query("equipment") equipment: String? = null
    ): Response<List<ExerciseDto>>

    @GET("api/users/me/preferences")
    suspend fun getPreferences(): Response<PreferencesDto>

    @PUT("api/users/me/preferences")
    suspend fun updatePreferences(@Body preferences: PreferencesDto): Response<PreferencesDto>

    @GET("api/users/me/progress")
    suspend fun getProgress(): Response<ProgressResponse>

    @GET("api/users/me/badges")
    suspend fun getBadges(): Response<List<BadgeDto>>

    @GET("api/workouts/suggested")
    suspend fun getSuggestedWorkout(
        @Query("recoveryHours") recoveryHours: Int = 48
    ): Response<SuggestedWorkoutResponse>

    @POST("api/workouts")
    suspend fun createWorkout(@Body request: CreateWorkoutRequest): Response<CreateWorkoutResponse>

    @POST("api/workouts/{id}/exercises")
    suspend fun addExercise(
        @Path("id") workoutId: String,
        @Body request: AddExerciseRequest
    ): Response<AddExerciseResponse>

    @POST("api/workouts/{id}/exercises/{weId}/sets")
    suspend fun logSet(
        @Path("id") workoutId: String,
        @Path("weId") workoutExerciseId: String,
        @Body request: LogSetRequest
    ): Response<LogSetResponse>

    @PATCH("api/workouts/{id}/complete")
    suspend fun completeWorkout(
        @Path("id") workoutId: String,
        @Body request: CompleteWorkoutRequest
    ): Response<CompleteWorkoutResponse>
}
