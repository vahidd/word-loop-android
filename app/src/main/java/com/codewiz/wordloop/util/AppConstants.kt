package com.codewiz.wordloop.util

import com.codewiz.wordloop.BuildConfig

object AppConstants {
    const val APP_NAME = "Word Loop"
    const val DEFAULT_WORD_LANGUAGE = "English"

    const val PRODUCTION_BASE_URL = BuildConfig.PRODUCTION_BASE_URL
    const val DEV_BASE_URL = BuildConfig.DEV_BASE_URL
    const val DEFAULT_BASE_URL = BuildConfig.DEFAULT_BASE_URL

    val testUserIds = setOf(
        "DbSEG4SvNHXMF59Tzj1K0PRM0Sm1",
        "FiRCpr3bTNgsYdfJvHpRYAwhxPi2",
        "7G17P27DitZyGF7ghqYEk7yJUaz2",
    )

    fun isTestUser(userId: String?, debug: Boolean = BuildConfig.DEBUG): Boolean {
        if (debug) return true
        return userId != null && userId in testUserIds
    }

    object StorageKeys {
        const val BACKEND_BASE_URL = "backendBaseURL"
        const val DEFAULT_WORD_LANGUAGE = "defaultWordLanguage"
        const val LEARNING_WORD_LANGUAGES = "learningWordLanguages"
        const val LANGUAGE_PROFICIENCY_LEVELS = "languageProficiencyLevels"
        const val DEFAULT_NATIVE_LANGUAGE = "defaultNativeLanguage"
        const val HAS_COMPLETED_ONBOARDING = "hasCompletedOnboarding"
        const val NOTIFICATIONS_ENABLED = "notificationsEnabled"
        const val MARKETING_NOTIFICATIONS_ENABLED = "marketingNotificationsEnabled"
        const val WORD_OF_THE_DAY_NOTIFICATIONS_ENABLED = "wordOfTheDayNotificationsEnabled"
        const val SOUND_EFFECTS_ENABLED = "soundEffectsEnabled"
        const val APP_LANGUAGE = "appLanguage"
        const val REVIEW_WORDS_ADDED_COUNT = "reviewWordsAddedCount"
        const val REVIEW_HAS_VIEWED_WORD_DETAIL = "reviewHasViewedWordDetail"
        const val REVIEW_SESSIONS_COMPLETED_COUNT = "reviewSessionsCompletedCount"
        const val REVIEW_HAS_MASTERED_WORD = "reviewHasMasteredWord"
        const val REVIEW_TRIGGER_1_FIRED = "reviewTrigger1Fired"
        const val REVIEW_TRIGGER_2_FIRED = "reviewTrigger2Fired"
        const val REVIEW_TRIGGER_3_FIRED = "reviewTrigger3Fired"
    }

    object Suggestions {
        const val TODAY_WORD_THRESHOLD = 5
        const val LIBRARY_WORD_THRESHOLD = 10
    }

    object Xp {
        const val CORRECT_ANSWER = 10
        const val REVIEW_SESSION_COMPLETE = 20
        const val WORD_MASTERED = 50
    }

    val supportedLanguages = listOf(
        "English",
        "Mandarin Chinese",
        "Hindi",
        "Spanish",
        "French",
        "Arabic",
        "Bengali",
        "Portuguese",
        "Russian",
        "Japanese",
        "German",
        "Korean",
        "Vietnamese",
        "Turkish",
        "Italian",
        "Thai",
        "Polish",
        "Ukrainian",
        "Dutch",
        "Indonesian",
        "Romanian",
        "Greek",
        "Czech",
        "Swedish",
        "Hungarian",
        "Hebrew",
        "Persian",
        "Malay",
        "Tamil",
        "Urdu",
    ).sorted()

    val languageBcp47 = mapOf(
        "English" to "en-US",
        "Mandarin Chinese" to "zh-CN",
        "Hindi" to "hi-IN",
        "Spanish" to "es-ES",
        "French" to "fr-FR",
        "Arabic" to "ar-SA",
        "Bengali" to "bn-IN",
        "Portuguese" to "pt-BR",
        "Russian" to "ru-RU",
        "Japanese" to "ja-JP",
        "German" to "de-DE",
        "Korean" to "ko-KR",
        "Vietnamese" to "vi-VN",
        "Turkish" to "tr-TR",
        "Italian" to "it-IT",
        "Thai" to "th-TH",
        "Polish" to "pl-PL",
        "Ukrainian" to "uk-UA",
        "Dutch" to "nl-NL",
        "Indonesian" to "id-ID",
        "Romanian" to "ro-RO",
        "Greek" to "el-GR",
        "Czech" to "cs-CZ",
        "Swedish" to "sv-SE",
        "Hungarian" to "hu-HU",
        "Hebrew" to "he-IL",
        "Persian" to "fa-IR",
        "Malay" to "ms-MY",
        "Tamil" to "ta-IN",
        "Urdu" to "ur-PK",
    )
}

fun String.wordInputFormatted(): String = trim()
