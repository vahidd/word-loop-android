package com.codewiz.wordloop.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codewiz.wordloop.BuildConfig
import com.codewiz.wordloop.domain.model.LanguageProficiency
import com.codewiz.wordloop.domain.model.UserProfile
import com.codewiz.wordloop.util.AppConstants
import com.codewiz.wordloop.util.AppUiLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "word_loop")

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val backendUrl = stringPreferencesKey(AppConstants.StorageKeys.BACKEND_BASE_URL)
    private val defaultWordLanguage = stringPreferencesKey(AppConstants.StorageKeys.DEFAULT_WORD_LANGUAGE)
    private val learningLanguages = stringPreferencesKey(AppConstants.StorageKeys.LEARNING_WORD_LANGUAGES)
    private val proficiency = stringPreferencesKey(AppConstants.StorageKeys.LANGUAGE_PROFICIENCY_LEVELS)
    private val nativeLanguage = stringPreferencesKey(AppConstants.StorageKeys.DEFAULT_NATIVE_LANGUAGE)
    private val hasOnboarded = booleanPreferencesKey(AppConstants.StorageKeys.HAS_COMPLETED_ONBOARDING)
    private val notifications = booleanPreferencesKey(AppConstants.StorageKeys.NOTIFICATIONS_ENABLED)
    private val marketing = booleanPreferencesKey(AppConstants.StorageKeys.MARKETING_NOTIFICATIONS_ENABLED)
    private val wotdNotifications =
        booleanPreferencesKey(AppConstants.StorageKeys.WORD_OF_THE_DAY_NOTIFICATIONS_ENABLED)
    private val soundEffects = booleanPreferencesKey(AppConstants.StorageKeys.SOUND_EFFECTS_ENABLED)
    private val appLanguage = stringPreferencesKey(AppConstants.StorageKeys.APP_LANGUAGE)
    private val wordsAdded = intPreferencesKey(AppConstants.StorageKeys.REVIEW_WORDS_ADDED_COUNT)
    private val viewedDetail = booleanPreferencesKey(AppConstants.StorageKeys.REVIEW_HAS_VIEWED_WORD_DETAIL)
    private val sessionsCompleted = intPreferencesKey(AppConstants.StorageKeys.REVIEW_SESSIONS_COMPLETED_COUNT)
    private val masteredWord = booleanPreferencesKey(AppConstants.StorageKeys.REVIEW_HAS_MASTERED_WORD)
    private val trigger1 = booleanPreferencesKey(AppConstants.StorageKeys.REVIEW_TRIGGER_1_FIRED)
    private val trigger2 = booleanPreferencesKey(AppConstants.StorageKeys.REVIEW_TRIGGER_2_FIRED)
    private val trigger3 = booleanPreferencesKey(AppConstants.StorageKeys.REVIEW_TRIGGER_3_FIRED)

    val soundEffectsEnabled: Flow<Boolean> = store.data.map { it[soundEffects] ?: true }
    val appLanguageCode: Flow<String?> = store.data.map { it[appLanguage] }
    val reviewRemindersEnabled: Flow<Boolean> = store.data.map { it[notifications] ?: false }
    val marketingEnabled: Flow<Boolean> = store.data.map { it[marketing] ?: false }
    val wordOfTheDayEnabled: Flow<Boolean> = store.data.map { it[wotdNotifications] ?: false }

    suspend fun baseUrl(): String {
        val stored = store.data.first()[backendUrl]?.trim()?.trimEnd('/')
        return if (stored.isNullOrEmpty()) BuildConfig.DEFAULT_BASE_URL.trimEnd('/') else stored
    }

    suspend fun setBaseUrl(url: String) {
        store.edit { it[backendUrl] = url.trim().trimEnd('/') }
    }

    suspend fun soundEffectsEnabledValue(): Boolean = store.data.first()[soundEffects] ?: true

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        store.edit { it[soundEffects] = enabled }
    }

    suspend fun setReviewRemindersEnabled(enabled: Boolean) {
        store.edit { it[notifications] = enabled }
    }

    suspend fun setMarketingEnabled(enabled: Boolean) {
        store.edit { it[marketing] = enabled }
    }

    suspend fun setWordOfTheDayEnabled(enabled: Boolean) {
        store.edit { it[wotdNotifications] = enabled }
    }

    suspend fun anyNotificationPreferenceEnabled(): Boolean {
        val prefs = store.data.first()
        return (prefs[notifications] == true) ||
            (prefs[marketing] == true) ||
            (prefs[wotdNotifications] == true)
    }

    suspend fun appLanguage(): AppUiLanguage {
        return AppUiLanguage.from(store.data.first()[appLanguage])
            ?: AppUiLanguage.fromSystemPreferred()
    }

    suspend fun setAppLanguage(language: AppUiLanguage) {
        store.edit { it[appLanguage] = language.code }
    }

    suspend fun lastAddWordLanguage(): String? = store.data.first()[defaultWordLanguage]

    suspend fun recordLastAddWordLanguage(language: String) {
        if (language in AppConstants.supportedLanguages) {
            store.edit { it[defaultWordLanguage] = language }
        }
    }

    suspend fun readProfileCache(): UserProfile {
        val prefs = store.data.first()
        val languages = decodeLanguages(prefs[learningLanguages])
        val levels = decodeProficiency(prefs[proficiency])
        val native = prefs[nativeLanguage]?.trim().orEmpty().ifEmpty { null }
        return UserProfile(
            hasCompletedOnboarding = prefs[hasOnboarded] ?: false,
            nativeLanguage = native,
            learningLanguages = languages.ifEmpty { listOf(AppConstants.DEFAULT_WORD_LANGUAGE) },
            proficiencyByLanguage = levels,
            appLanguage = prefs[appLanguage] ?: AppUiLanguage.fromSystemPreferred().code,
        )
    }

    suspend fun applyProfile(profile: UserProfile) {
        store.edit {
            it[hasOnboarded] = profile.hasCompletedOnboarding
            it[learningLanguages] = json.encodeToString(profile.learningLanguages)
            it[proficiency] = json.encodeToString(profile.proficiencyByLanguage)
            it[nativeLanguage] = profile.nativeLanguage.orEmpty()
            it[appLanguage] = profile.appLanguage
        }
    }

    suspend fun clearProfile() {
        store.edit {
            it.remove(hasOnboarded)
            it.remove(learningLanguages)
            it.remove(proficiency)
            it.remove(nativeLanguage)
            it.remove(appLanguage)
        }
    }

    suspend fun shouldMigrateLocalToServer(server: UserProfile): Boolean {
        val local = readProfileCache()
        return local.hasCompletedOnboarding && !server.hasCompletedOnboarding
    }

    fun nativeLanguageForApi(native: String?): String? {
        val trimmed = native?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.equals(AppConstants.DEFAULT_WORD_LANGUAGE, ignoreCase = true)) return null
        return trimmed
    }

    suspend fun reviewSnapshot(): ReviewSnapshot {
        val prefs = store.data.first()
        return ReviewSnapshot(
            wordsAddedCount = prefs[wordsAdded] ?: 0,
            hasViewedWordDetail = prefs[viewedDetail] ?: false,
            reviewSessionsCompletedCount = prefs[sessionsCompleted] ?: 0,
            hasMasteredWord = prefs[masteredWord] ?: false,
            trigger1Fired = prefs[trigger1] ?: false,
            trigger2Fired = prefs[trigger2] ?: false,
            trigger3Fired = prefs[trigger3] ?: false,
        )
    }

    suspend fun writeReviewSnapshot(snapshot: ReviewSnapshot) {
        store.edit {
            it[wordsAdded] = snapshot.wordsAddedCount
            it[viewedDetail] = snapshot.hasViewedWordDetail
            it[sessionsCompleted] = snapshot.reviewSessionsCompletedCount
            it[masteredWord] = snapshot.hasMasteredWord
            it[trigger1] = snapshot.trigger1Fired
            it[trigger2] = snapshot.trigger2Fired
            it[trigger3] = snapshot.trigger3Fired
        }
    }

    private fun decodeLanguages(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
            .filter { it in AppConstants.supportedLanguages }
            .distinct()
    }

    private fun decodeProficiency(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
            .filterKeys { it in AppConstants.supportedLanguages }
            .filterValues { value -> LanguageProficiency.entries.any { it.raw == value } }
    }
}

data class ReviewSnapshot(
    val wordsAddedCount: Int = 0,
    val hasViewedWordDetail: Boolean = false,
    val reviewSessionsCompletedCount: Int = 0,
    val hasMasteredWord: Boolean = false,
    val trigger1Fired: Boolean = false,
    val trigger2Fired: Boolean = false,
    val trigger3Fired: Boolean = false,
    val pendingReviewRequest: Boolean = false,
)
