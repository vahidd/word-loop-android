package com.codewiz.wordloop.data.api

import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.NotificationPreferences
import com.codewiz.wordloop.domain.model.UserProfile
import com.codewiz.wordloop.domain.model.UserProgress
import com.codewiz.wordloop.domain.model.WordOfTheDayStatus
import com.codewiz.wordloop.domain.model.WordSuggestion
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WordLoopApi {
    @GET("api/words")
    suspend fun listWords(
        @Query("status") status: String? = null,
        @Query("due") due: String? = null,
        @Query("language") language: String? = null,
    ): ApiSuccess<List<LearnedWord>>

    @GET("api/words/{id}")
    suspend fun getWord(@Path("id") id: String): ApiSuccess<LearnedWord>

    @POST("api/words")
    suspend fun createWord(@Body body: CreateWordBody): ApiSuccess<LearnedWord>

    @POST("api/words/{id}/regenerate")
    suspend fun regenerateWord(@Path("id") id: String): ApiSuccess<LearnedWord>

    @POST("api/words/{id}/examples")
    suspend fun generateMoreExamples(@Path("id") id: String): ApiSuccess<LearnedWord>

    @PATCH("api/words/{id}")
    suspend fun updateWord(@Path("id") id: String, @Body body: UpdateWordBody): ApiSuccess<LearnedWord>

    @DELETE("api/words/{id}")
    suspend fun deleteWord(@Path("id") id: String): Response<Unit>

    @GET("api/words/export")
    suspend fun exportWords(): ApiSuccess<List<ImportWordEntry>>

    @POST("api/words/import")
    suspend fun importWords(@Body body: ImportWordsBody): ApiSuccess<ImportWordsResponse>

    @GET("api/progress")
    suspend fun getProgress(): ApiSuccess<UserProgress>

    @GET("api/suggestions")
    suspend fun getSuggestions(
        @Query("language") language: String,
        @Query("level") level: String,
        @Query("nativeLanguage") nativeLanguage: String? = null,
    ): ApiSuccess<List<WordSuggestion>>

    @GET("api/word-of-the-day")
    suspend fun getWordOfTheDay(@Query("timezone") timezone: String): ApiSuccess<WordOfTheDayStatus>

    @POST("api/reviews/answers")
    suspend fun submitReviewAnswer(@Body body: SubmitReviewAnswerBody): ApiSuccess<ReviewAnswerResponse>

    @POST("api/reviews/sessions/complete")
    suspend fun completeReviewSession(): ApiSuccess<ReviewSessionCompleteResponse>

    @GET("api/user/profile")
    suspend fun getUserProfile(): ApiSuccess<UserProfile>

    @PATCH("api/user/profile")
    suspend fun updateUserProfile(@Body body: UpdateUserProfileBody): ApiSuccess<UserProfile>

    @DELETE("api/user/data")
    suspend fun resetUserData(): Response<Unit>

    @DELETE("api/user/account")
    suspend fun deleteAccount(): Response<Unit>

    @GET("api/notifications/preferences")
    suspend fun getNotificationPreferences(): ApiSuccess<NotificationPreferences>

    @PATCH("api/notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Body body: UpdateNotificationPreferencesBody,
    ): ApiSuccess<NotificationPreferences>

    @POST("api/notifications/devices")
    suspend fun registerDevice(@Body body: RegisterDeviceBody): Response<Unit>

    @HTTP(method = "DELETE", path = "api/notifications/devices", hasBody = true)
    suspend fun unregisterDevice(@Body body: UnregisterDeviceBody): Response<Unit>
}

interface TrackEventApi {
    @POST("web/api/track-event/")
    suspend fun track(@Body body: TrackEventBody)
}
