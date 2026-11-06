package com.fitquest.app.data.remote

import com.fitquest.app.data.remote.dto.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit definition of the FitQuest REST API.
 *
 * Reference: Square (2026) Retrofit. https://square.github.io/retrofit/
 */
interface FitQuestApi {

    @POST("api/auth/sso")
    suspend fun ssoSignIn(@Body request: SsoRequest): Response<SsoResponse>

    @GET("api/users/me")
    suspend fun getProfile(): Response<UserDto>

    @DELETE("api/users/me")
    suspend fun deleteAccount(): Response<Unit>

    @GET("api/users/me/routines")
    suspend fun getRoutines(): Response<RoutinesPayload>

    @PUT("api/users/me/routines")
    suspend fun saveRoutines(@Body payload: RoutinesPayload): Response<RoutinesPayload>

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

    @POST("api/users/me/personal-records/reset")
    suspend fun resetPersonalRecords(): Response<ResetRecordsResponse>

    @GET("api/badges")
    suspend fun getBadgeCatalog(): Response<List<BadgeDto>>

    @GET("api/workouts/suggested")
    suspend fun getSuggestedWorkout(
        @Query("recoveryHours") recoveryHours: Int = 48
    ): Response<SuggestedWorkoutResponse>

    @POST("api/workouts/sync")
    suspend fun syncWorkout(@Body request: SyncWorkoutRequest): Response<SyncWorkoutResponse>

    @GET("api/workouts/history")
    suspend fun getWorkoutHistory(): Response<List<HistoryWorkoutDto>>

    @POST("api/users/me/device-tokens")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): Response<Unit>

    @POST("api/users/me/notifications/test")
    suspend fun sendTestNotification(): Response<TestNotificationResponse>

    @POST("api/users/me/avatar")
    suspend fun uploadAvatar(@Body image: RequestBody): Response<UserDto>

    @GET("api/users/me/photos")
    suspend fun listPhotos(): Response<List<ProgressPhotoDto>>

    @POST("api/users/me/photos")
    suspend fun uploadPhoto(@Body image: RequestBody): Response<ProgressPhotoDto>

    @DELETE("api/users/me/photos/{id}")
    suspend fun deletePhoto(@Path("id") photoId: String): Response<Unit>
}
