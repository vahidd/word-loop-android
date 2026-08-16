package com.codewiz.wordloop.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class WordStatus(val raw: String) {
    NEW("new"),
    LEARNING("learning"),
    DIFFICULT("difficult"),
    MASTERED("mastered"),
    ARCHIVED("archived"),
    ;

    val displayName: String
        get() = when (this) {
            NEW -> "New"
            LEARNING -> "Learning"
            DIFFICULT -> "Difficult"
            MASTERED -> "Mastered"
            ARCHIVED -> "Archived"
        }

    val isReviewable: Boolean get() = this != ARCHIVED

    companion object {
        fun from(raw: String): WordStatus = entries.firstOrNull { it.raw == raw } ?: NEW
    }
}

enum class ReviewMode(val raw: String) {
    STANDARD("standard"),
    INTENSIVE("intensive"),
    ;

    companion object {
        fun from(raw: String?): ReviewMode =
            entries.firstOrNull { it.raw == raw } ?: STANDARD
    }
}

enum class LanguageProficiency(val raw: String) {
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced"),
    ;

    val displayName: String
        get() = when (this) {
            BEGINNER -> "Beginner"
            INTERMEDIATE -> "Intermediate"
            ADVANCED -> "Advanced"
        }

    val subtitle: String
        get() = when (this) {
            BEGINNER -> "Just starting out or know a few basics"
            INTERMEDIATE -> "Can hold conversations with some gaps"
            ADVANCED -> "Comfortable but building vocabulary depth"
        }

    companion object {
        fun from(raw: String?): LanguageProficiency =
            entries.firstOrNull { it.raw == raw } ?: BEGINNER
    }
}

@Serializable
data class Pronunciation(
    val ipa: String? = null,
    val simple: String? = null,
    val audioUrl: String? = null,
)

@Serializable
data class WordExample(
    val id: String? = null,
    val sentence: String = "",
    val meaning: String = "",
)

@Serializable
data class CommonPhrase(
    val id: String? = null,
    val phrase: String = "",
    val meaning: String = "",
)

@Serializable
data class OtherMeaning(
    val partOfSpeech: String = "",
    val shortMeaning: String = "",
    val detailedMeaning: String = "",
    val examples: List<WordExample> = emptyList(),
)

@Serializable
data class WordQuiz(
    val id: String,
    val type: String,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
)

@Serializable
data class LearnedWord(
    val id: String,
    val uniqueKey: String = "",
    val word: String,
    val language: String,
    val normalizedWord: String = word,
    val partOfSpeech: String? = null,
    val difficulty: String? = null,
    val shortMeaning: String = "",
    val detailedMeaning: String = "",
    val otherMeanings: List<OtherMeaning> = emptyList(),
    val meaningsInNativeLang: List<String> = emptyList(),
    val pronunciation: Pronunciation? = null,
    val memoryHint: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val examples: List<WordExample> = emptyList(),
    val commonPhrases: List<CommonPhrase> = emptyList(),
    val quizzes: List<WordQuiz> = emptyList(),
    val status: String = "new",
    val correctAnswersCount: Int = 0,
    val wrongAnswersCount: Int = 0,
    val repetitionCount: Int = 0,
    val easinessFactor: Double = 2.5,
    val interval: Int = 0,
    val nextReviewDate: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val masteredAt: String? = null,
    val isDue: Boolean = false,
) {
    val wordStatus: WordStatus get() = WordStatus.from(status)
    val isReviewable: Boolean get() = wordStatus.isReviewable
    val pronunciationIpa: String? get() = pronunciation?.ipa
    val pronunciationSimple: String? get() = pronunciation?.simple
    val pronunciationAudioUrl: String? get() = pronunciation?.audioUrl

    fun matchesWordIdentity(word: String, language: String): Boolean =
        this.word == word && this.language == language
}

@Serializable
data class UserProfile(
    val hasCompletedOnboarding: Boolean = false,
    val nativeLanguage: String? = null,
    val learningLanguages: List<String> = listOf("English"),
    val proficiencyByLanguage: Map<String, String> = emptyMap(),
    val reviewMode: String = ReviewMode.STANDARD.raw,
    val appLanguage: String = "en",
) {
    val reviewModeEnum: ReviewMode get() = ReviewMode.from(reviewMode)

    fun proficiency(language: String): LanguageProficiency =
        LanguageProficiency.from(proficiencyByLanguage[language])
}

@Serializable
data class UserProgressStats(
    val totalWords: Int = 0,
    val learningCount: Int = 0,
    val difficultCount: Int = 0,
    val masteredCount: Int = 0,
    val archivedCount: Int = 0,
    val accuracy: Int? = null,
)

@Serializable
data class UserProgress(
    val xp: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastReviewCompletionDate: String? = null,
    val totalReviewSessions: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val totalWrongAnswers: Int = 0,
    val stats: UserProgressStats = UserProgressStats(),
)

@Serializable
data class WordSuggestion(
    val word: String,
    val meaning: String = "",
)

@Serializable
data class WordOfTheDay(
    val id: String,
    val language: String,
    val level: String = "beginner",
    val date: String = "",
    val word: String,
    val normalizedWord: String = word,
    val partOfSpeech: String? = null,
    val difficulty: String? = null,
    val shortMeaning: String = "",
    val detailedMeaning: String = "",
    val meaningsInNativeLang: List<String> = emptyList(),
    val pronunciation: Pronunciation? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val examples: List<WordExample> = emptyList(),
    val commonPhrases: List<CommonPhrase> = emptyList(),
    val memoryHint: String? = null,
    val added: Boolean = false,
    val addedWordId: String? = null,
)

@Serializable
data class WordOfTheDayStatus(
    val active: Boolean = false,
    val currentWords: Int = 0,
    val requiredWords: Int = 0,
    val words: List<WordOfTheDay> = emptyList(),
)

@Serializable
data class NotificationPreferences(
    val reviewRemindersEnabled: Boolean = false,
    val marketingEnabled: Boolean = false,
    val wordOfTheDayEnabled: Boolean = false,
    val timezone: String = "UTC",
    val quietHoursStart: Int = 9,
    val quietHoursEnd: Int = 21,
)

fun intervalDescription(intervalInDays: Int): String = when {
    intervalInDays <= 0 -> "Not scheduled"
    intervalInDays == 1 -> "1 day"
    else -> "$intervalInDays days"
}
