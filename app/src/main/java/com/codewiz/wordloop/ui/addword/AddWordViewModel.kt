package com.codewiz.wordloop.ui.addword

import androidx.lifecycle.ViewModel
import com.codewiz.wordloop.data.api.ApiError
import com.codewiz.wordloop.data.prefs.UserPrefs
import com.codewiz.wordloop.data.review.ReviewRequestManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.data.store.userMessage
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.util.AppConstants
import com.codewiz.wordloop.util.wordInputFormatted
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UnrecognizedWord(
    val attemptedWord: String,
    val language: String,
    val suggestion: String?,
    val message: String,
)

data class AddWordUiState(
    val word: String = "",
    val selectedLanguage: String = AppConstants.DEFAULT_WORD_LANGUAGE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val generatedWord: LearnedWord? = null,
    val existingSenses: List<LearnedWord> = emptyList(),
    val unrecognizedWord: UnrecognizedWord? = null,
)

@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val store: WordLoopStore,
    private val prefs: UserPrefs,
    private val reviewRequests: ReviewRequestManager,
) : ViewModel() {
    private val _state = MutableStateFlow(AddWordUiState())
    val state: StateFlow<AddWordUiState> = _state.asStateFlow()

    suspend fun prepare() {
        val profile = store.userProfile.value
        val languages = profile?.learningLanguages.orEmpty()
        val last = prefs.lastAddWordLanguage()
        val selected = when {
            last != null && last in languages -> last
            languages.isNotEmpty() -> languages.first()
            else -> AppConstants.DEFAULT_WORD_LANGUAGE
        }
        _state.update { it.copy(selectedLanguage = selected) }
    }

    fun updateWord(value: String) {
        _state.update { it.copy(word = value, unrecognizedWord = null, errorMessage = null) }
    }

    fun selectLanguage(language: String) {
        _state.update { it.copy(selectedLanguage = language) }
    }

    fun reset() {
        _state.value = AddWordUiState(selectedLanguage = _state.value.selectedLanguage)
    }

    fun viewExisting(sense: LearnedWord) {
        _state.update { it.copy(generatedWord = sense, existingSenses = emptyList()) }
    }

    fun dismissExisting() {
        _state.update { it.copy(existingSenses = emptyList()) }
    }

    suspend fun generate() {
        val prepared = _state.value.word.wordInputFormatted()
        val language = _state.value.selectedLanguage
        if (prepared.isEmpty() || language.isEmpty()) return
        _state.update { it.copy(word = prepared) }
        val existing = store.words.value.filter { it.matchesWordIdentity(prepared, language) }
        if (existing.isNotEmpty()) {
            _state.update { it.copy(existingSenses = existing) }
            return
        }
        performGeneration(prepared, language)
    }

    suspend fun useSuggestion() {
        val suggestion = _state.value.unrecognizedWord?.suggestion?.wordInputFormatted() ?: return
        _state.update { it.copy(word = suggestion, unrecognizedWord = null) }
        performGeneration(suggestion, _state.value.selectedLanguage)
    }

    suspend fun regenerateExisting(sense: LearnedWord) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val updated = store.regenerateWord(sense)
            _state.update { it.copy(generatedWord = updated, word = "", existingSenses = emptyList()) }
        } catch (error: Exception) {
            _state.update { it.copy(errorMessage = error.userMessage()) }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun performGeneration(word: String, language: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null, unrecognizedWord = null) }
        try {
            val created = store.createWord(word, language)
            reviewRequests.recordWordAdded()
            prefs.recordLastAddWordLanguage(language)
            _state.update { it.copy(generatedWord = created, word = "") }
        } catch (error: ApiError.WordNotRecognized) {
            _state.update {
                it.copy(
                    unrecognizedWord = UnrecognizedWord(
                        attemptedWord = word,
                        language = language,
                        suggestion = ApiError.suggestionFrom(error.suggestion, error.detail),
                        message = error.detail,
                    ),
                )
            }
        } catch (error: ApiError.Server) {
            if (error.code == "WORD_ALREADY_EXISTS") {
                _state.update {
                    it.copy(existingSenses = store.words.value.filter { w -> w.matchesWordIdentity(word, language) })
                }
            } else {
                _state.update { it.copy(errorMessage = error.userMessage()) }
            }
        } catch (error: Exception) {
            _state.update { it.copy(errorMessage = error.userMessage()) }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
}
