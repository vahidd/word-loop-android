package com.codewiz.wordloop.data.store

import com.codewiz.wordloop.data.analytics.AnalyticsTracker
import com.codewiz.wordloop.data.api.ApiError
import com.codewiz.wordloop.data.api.CreateWordBody
import com.codewiz.wordloop.data.api.ImportWordEntry
import com.codewiz.wordloop.data.api.ImportWordsBody
import com.codewiz.wordloop.data.api.ImportWordsResponse
import com.codewiz.wordloop.data.api.ReviewAnswerResponse
import com.codewiz.wordloop.data.api.ReviewSessionCompleteResponse
import com.codewiz.wordloop.data.api.SubmitReviewAnswerBody
import com.codewiz.wordloop.data.api.UpdateNotificationPreferencesBody
import com.codewiz.wordloop.data.api.UpdateUserProfileBody
import com.codewiz.wordloop.data.api.UpdateWordBody
import com.codewiz.wordloop.data.api.WordLoopApi
import com.codewiz.wordloop.data.prefs.UserPrefs
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.NotificationPreferences
import com.codewiz.wordloop.domain.model.UserProfile
import com.codewiz.wordloop.domain.model.UserProgress
import com.codewiz.wordloop.domain.model.WordOfTheDay
import com.codewiz.wordloop.domain.model.WordQuiz
import com.codewiz.wordloop.domain.model.WordSuggestion
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.codewiz.wordloop.di.readApiError
import java.io.IOException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class WordLoopStore @Inject constructor(
    private val api: WordLoopApi,
    private val prefs: UserPrefs,
    private val analytics: AnalyticsTracker,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val refreshMutex = Mutex()

    private val _words = MutableStateFlow<List<LearnedWord>>(emptyList())
    val words: StateFlow<List<LearnedWord>> = _words.asStateFlow()

    private val _dueWords = MutableStateFlow<List<LearnedWord>>(emptyList())
    val dueWords: StateFlow<List<LearnedWord>> = _dueWords.asStateFlow()

    private val _progress = MutableStateFlow<UserProgress?>(null)
    val progress: StateFlow<UserProgress?> = _progress.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _wordOfTheDay = MutableStateFlow<List<WordOfTheDay>>(emptyList())
    val wordOfTheDay: StateFlow<List<WordOfTheDay>> = _wordOfTheDay.asStateFlow()

    private val _wordOfTheDayActive = MutableStateFlow(false)
    val wordOfTheDayActive: StateFlow<Boolean> = _wordOfTheDayActive.asStateFlow()

    private val _wordOfTheDayCurrentWords = MutableStateFlow(0)
    val wordOfTheDayCurrentWords: StateFlow<Int> = _wordOfTheDayCurrentWords.asStateFlow()

    private val _wordOfTheDayRequiredWords = MutableStateFlow(0)
    val wordOfTheDayRequiredWords: StateFlow<Int> = _wordOfTheDayRequiredWords.asStateFlow()

    private val _isLoadingWordOfTheDay = MutableStateFlow(false)
    val isLoadingWordOfTheDay: StateFlow<Boolean> = _isLoadingWordOfTheDay.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suggestions = MutableStateFlow<Map<String, List<WordSuggestion>>>(emptyMap())
    val suggestions: StateFlow<Map<String, List<WordSuggestion>>> = _suggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    suspend fun refreshAll() {
        refreshMutex.withLock {
            _isLoading.value = true
            try {
                analytics.identifyCurrentUser()
                analytics.log(AnalyticsTracker.Event.APP_REFRESH)
                coroutineScope {
                    val wordsJob = async { fetch { api.listWords() } }
                    val dueJob = async { fetch { api.listWords(due = "true") } }
                    val progressJob = async { fetch { api.getProgress() } }
                    val profileJob = async { refreshProfileFromServer() }
                    _words.value = wordsJob.await()
                    _dueWords.value = dueJob.await()
                    _progress.value = progressJob.await()
                    profileJob.await()
                }
                _lastError.value = null
            } catch (error: Exception) {
                analytics.record(error)
                _lastError.value = error.userMessage()
            } finally {
                _isLoading.value = false
            }
            scope.launch { refreshWordOfTheDay() }
        }
    }

    suspend fun refreshWordOfTheDay() {
        _isLoadingWordOfTheDay.value = true
        try {
            val status = fetch {
                api.getWordOfTheDay(timezone = TimeZone.getDefault().id)
            }
            _wordOfTheDayActive.value = status.active
            _wordOfTheDayCurrentWords.value = status.currentWords
            _wordOfTheDayRequiredWords.value = status.requiredWords
            _wordOfTheDay.value = status.words
        } catch (error: Exception) {
            _lastError.value = error.userMessage()
        } finally {
            _isLoadingWordOfTheDay.value = false
        }
    }

    suspend fun refreshProfileFromServer(): UserProfile? {
        return try {
            var profile = fetch { api.getUserProfile() }
            if (prefs.shouldMigrateLocalToServer(profile)) {
                val local = prefs.readProfileCache()
                profile = unwrap(
                    api.updateUserProfile(
                        UpdateUserProfileBody(
                            hasCompletedOnboarding = local.hasCompletedOnboarding,
                            nativeLanguage = local.nativeLanguage ?: profile.nativeLanguage,
                            learningLanguages = local.learningLanguages.ifEmpty { profile.learningLanguages },
                            proficiencyByLanguage = local.proficiencyByLanguage.ifEmpty {
                                profile.proficiencyByLanguage
                            },
                            appLanguage = local.appLanguage,
                        ),
                    ),
                )
            }
            applyProfile(profile)
            profile
        } catch (error: Exception) {
            _lastError.value = error.userMessage()
            null
        }
    }

    suspend fun updateUserProfile(body: UpdateUserProfileBody): UserProfile {
        val profile = fetch { api.updateUserProfile(body) }
        applyProfile(profile)
        _lastError.value = null
        return profile
    }

    suspend fun saveOnboardingProfile(
        learningLanguages: List<String>,
        proficiency: Map<String, String>,
        nativeLanguage: String,
        hasCompletedOnboarding: Boolean = true,
    ): UserProfile {
        val synced = proficiency.filterKeys { it in learningLanguages }.toMutableMap()
        learningLanguages.forEach { language ->
            if (language !in synced) synced[language] = "beginner"
        }
        return updateUserProfile(
            UpdateUserProfileBody(
                hasCompletedOnboarding = hasCompletedOnboarding,
                nativeLanguage = nativeLanguage,
                learningLanguages = learningLanguages,
                proficiencyByLanguage = synced,
            ),
        )
    }

    suspend fun refreshWords() {
        try {
            _words.value = fetch { api.listWords() }
            _dueWords.value = fetch { api.listWords(due = "true") }
            _lastError.value = null
        } catch (error: Exception) {
            _lastError.value = error.userMessage()
        }
    }

    suspend fun refreshProgress() {
        try {
            _progress.value = fetch { api.getProgress() }
            _lastError.value = null
        } catch (error: Exception) {
            _lastError.value = error.userMessage()
        }
    }

    suspend fun createWord(word: String, language: String): LearnedWord {
        val created = fetch { api.createWord(CreateWordBody(word = word, language = language)) }
        upsertWord(created)
        refreshProgress()
        return created
    }

    suspend fun regenerateWord(word: LearnedWord): LearnedWord {
        val updated = fetch { api.regenerateWord(word.id) }
        upsertWord(updated)
        return updated
    }

    suspend fun generateMoreExamples(word: LearnedWord): LearnedWord {
        val updated = fetch { api.generateMoreExamples(word.id) }
        upsertWord(updated)
        return updated
    }

    suspend fun archiveWord(word: LearnedWord): LearnedWord {
        val updated = fetch { api.updateWord(word.id, UpdateWordBody("archive")) }
        upsertWord(updated)
        refreshDueWords()
        return updated
    }

    suspend fun unarchiveWord(word: LearnedWord): LearnedWord {
        val updated = fetch { api.updateWord(word.id, UpdateWordBody("unarchive")) }
        upsertWord(updated)
        refreshDueWords()
        return updated
    }

    suspend fun deleteWord(word: LearnedWord) {
        runApi { api.deleteWord(word.id).also { it.throwIfFailed() } }
        _words.value = _words.value.filterNot { it.id == word.id }
        _dueWords.value = _dueWords.value.filterNot { it.id == word.id }
        refreshProgress()
    }

    suspend fun submitReviewAnswer(
        word: LearnedWord,
        quiz: WordQuiz,
        selectedOptionIndex: Int,
    ): ReviewAnswerResponse {
        val response = fetch {
            api.submitReviewAnswer(
                SubmitReviewAnswerBody(
                    wordId = word.id,
                    quizId = quiz.id,
                    selectedOptionIndex = selectedOptionIndex,
                ),
            )
        }
        upsertWord(response.word)
        scope.launch {
            refreshDueWords()
            refreshProgress()
        }
        analytics.log(AnalyticsTracker.Event.REVIEW_SUBMITTED)
        return response
    }

    suspend fun completeReviewSession(): ReviewSessionCompleteResponse {
        val response = fetch { api.completeReviewSession() }
        refreshProgress()
        return response
    }

    suspend fun loadSuggestions() {
        val profile = _userProfile.value ?: return
        if (profile.learningLanguages.isEmpty()) return
        _isLoadingSuggestions.value = true
        try {
            val native = prefs.nativeLanguageForApi(profile.nativeLanguage)
            val loaded = coroutineScope {
                profile.learningLanguages.map { language ->
                    async {
                        val level = profile.proficiencyByLanguage[language] ?: "beginner"
                        language to runCatching {
                            fetch { api.getSuggestions(language, level, native) }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().toMap()
            }
            _suggestions.value = loaded
        } finally {
            _isLoadingSuggestions.value = false
        }
    }

    fun clearLocalState() {
        analytics.log(AnalyticsTracker.Event.SIGN_OUT)
        analytics.clearIdentity()
        _words.value = emptyList()
        _dueWords.value = emptyList()
        _progress.value = null
        _userProfile.value = null
        _wordOfTheDay.value = emptyList()
        _wordOfTheDayActive.value = false
        _wordOfTheDayCurrentWords.value = 0
        _wordOfTheDayRequiredWords.value = 0
        _suggestions.value = emptyMap()
        _lastError.value = null
        scope.launch { prefs.clearProfile() }
    }

    suspend fun resetAllData() {
        runApi { api.resetUserData().also { it.throwIfFailed() } }
        refreshAll()
    }

    suspend fun deleteAccount() {
        runApi { api.deleteAccount().also { it.throwIfFailed() } }
        clearLocalState()
    }

    suspend fun exportWords(): ByteArray {
        val entries = fetch { api.exportWords() }
        return json.encodeToString(entries).toByteArray()
    }

    suspend fun importWords(bytes: ByteArray): ImportWordsResponse {
        val entries = json.decodeFromString<List<ImportWordEntry>>(bytes.decodeToString())
        val result = fetch { api.importWords(ImportWordsBody(entries)) }
        refreshAll()
        return result
    }

    fun word(id: String): LearnedWord? = _words.value.firstOrNull { it.id == id }

    suspend fun notificationPreferences(): NotificationPreferences =
        fetch { api.getNotificationPreferences() }

    suspend fun updateNotificationPreferences(body: UpdateNotificationPreferencesBody): NotificationPreferences =
        fetch { api.updateNotificationPreferences(body) }

    private fun applyProfile(profile: UserProfile) {
        _userProfile.value = profile
        scope.launch { prefs.applyProfile(profile) }
    }

    private fun upsertWord(word: LearnedWord) {
        val current = _words.value.toMutableList()
        val index = current.indexOfFirst { it.id == word.id }
        if (index >= 0) current[index] = word else current.add(0, word)
        _words.value = current
    }

    private suspend fun refreshDueWords() {
        try {
            _dueWords.value = fetch { api.listWords(due = "true") }
        } catch (error: Exception) {
            _lastError.value = error.userMessage()
        }
    }

    private suspend fun <T> fetch(block: suspend () -> com.codewiz.wordloop.data.api.ApiSuccess<T>): T =
        unwrap(runApi(block))

    private suspend fun <T> runApi(block: suspend () -> T): T {
        return try {
            block()
        } catch (error: HttpException) {
            val body = error.response()?.errorBody()?.string()
            throw readApiError(json, body, error.code())
        } catch (error: IOException) {
            throw ApiError.Network(error.message ?: "Network error")
        }
    }

    private fun <T> unwrap(response: com.codewiz.wordloop.data.api.ApiSuccess<T>): T {
        return response.data ?: throw ApiError.Decoding("Response missing data field")
    }
}

private fun retrofit2.Response<*>.throwIfFailed() {
    if (!isSuccessful) {
        throw ApiError.UnexpectedStatus(code())
    }
}

fun Throwable.userMessage(): String = when (this) {
    is ApiError -> message ?: localizedMessage.orEmpty()
    is HttpException -> localizedMessage ?: "Unexpected server response (${code()})."
    else -> localizedMessage ?: "Something went wrong. Please try again."
}
