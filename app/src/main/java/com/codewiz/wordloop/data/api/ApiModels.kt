package com.codewiz.wordloop.data.api

import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.NotificationPreferences
import com.codewiz.wordloop.domain.model.UserProfile
import com.codewiz.wordloop.domain.model.UserProgress
import com.codewiz.wordloop.domain.model.WordOfTheDayStatus
import com.codewiz.wordloop.domain.model.WordSuggestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiSuccess<T>(
    val success: Boolean = true,
    val data: T? = null,
    val cached: Boolean? = null,
)

@Serializable
data class ApiErrorBody(
    val success: Boolean = false,
    val error: ApiErrorDetail? = null,
)

@Serializable
data class ApiErrorDetail(
    val code: String = "",
    val message: String = "",
    val suggestion: String? = null,
    val details: List<ApiValidationDetail> = emptyList(),
)

@Serializable
data class ApiValidationDetail(
    val field: String = "",
    val message: String = "",
)

@Serializable
data class CreateWordBody(
    val word: String,
    val language: String,
)

@Serializable
data class UpdateWordBody(
    val action: String,
)

@Serializable
data class SubmitReviewAnswerBody(
    val wordId: String,
    val quizId: String,
    val selectedOptionIndex: Int,
)

@Serializable
data class ReviewAnswerResult(
    val correct: Boolean,
    val xpEarned: Int = 0,
    val wordsAdvanced: Int = 0,
    val wordsReset: Int = 0,
    val wordsMastered: Int = 0,
)

@Serializable
data class ReviewAnswerResponse(
    val word: LearnedWord,
    val result: ReviewAnswerResult,
    val progress: JsonElement? = null,
)

@Serializable
data class ReviewSessionCompleteResponse(
    val xpEarned: Int = 0,
    val progress: UserProgress? = null,
)

@Serializable
data class ImportWordEntry(
    val word: String,
    val language: String,
)

@Serializable
data class ImportWordsBody(
    val entries: List<ImportWordEntry>,
)

@Serializable
data class ImportWordsResponse(
    val insertedWords: Int = 0,
    val skippedWords: Int = 0,
    val failedWords: List<String> = emptyList(),
)

@Serializable
data class UpdateUserProfileBody(
    val hasCompletedOnboarding: Boolean? = null,
    val nativeLanguage: String? = null,
    val learningLanguages: List<String>? = null,
    val proficiencyByLanguage: Map<String, String>? = null,
    val reviewMode: String? = null,
    val appLanguage: String? = null,
)

@Serializable
data class RegisterDeviceBody(
    val fcmToken: String,
    val platform: String = "android",
    val appVersion: String? = null,
)

@Serializable
data class UnregisterDeviceBody(
    val fcmToken: String,
)

@Serializable
data class UpdateNotificationPreferencesBody(
    val reviewRemindersEnabled: Boolean? = null,
    val marketingEnabled: Boolean? = null,
    val wordOfTheDayEnabled: Boolean? = null,
    val timezone: String? = null,
)

@Serializable
data class TrackEventBody(
    val event: String,
    val meta: Map<String, String> = emptyMap(),
)

typealias WordsResponse = ApiSuccess<List<LearnedWord>>
typealias WordResponse = ApiSuccess<LearnedWord>
typealias ProgressResponse = ApiSuccess<UserProgress>
typealias ProfileResponse = ApiSuccess<UserProfile>
typealias SuggestionsResponse = ApiSuccess<List<WordSuggestion>>
typealias WotdResponse = ApiSuccess<WordOfTheDayStatus>
typealias NotificationPrefsResponse = ApiSuccess<NotificationPreferences>
typealias ReviewAnswerApiResponse = ApiSuccess<ReviewAnswerResponse>
typealias ReviewSessionApiResponse = ApiSuccess<ReviewSessionCompleteResponse>
typealias ImportApiResponse = ApiSuccess<ImportWordsResponse>
typealias ExportResponse = ApiSuccess<List<ImportWordEntry>>
