package com.codewiz.wordloop.ui.quiz

import androidx.lifecycle.ViewModel
import com.codewiz.wordloop.data.audio.QuizSounds
import com.codewiz.wordloop.data.review.ReviewRequestManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.data.store.userMessage
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.domain.model.WordQuiz
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val store: WordLoopStore,
    private val sounds: QuizSounds,
    private val reviewRequests: ReviewRequestManager,
) : ViewModel() {
    enum class Mode { REVIEW, PRACTICE }
    enum class Phase { ANSWERING, SHOWING_FEEDBACK, SUMMARY }

    data class QuizItem(val word: LearnedWord, val quiz: WordQuiz)
    data class FailedWord(val word: LearnedWord, val correctAnswer: String)
    data class SessionResult(
        val totalQuestions: Int = 0,
        val correctCount: Int = 0,
        val wrongCount: Int = 0,
        val skippedCount: Int = 0,
        val xpEarned: Int = 0,
        val wordsAdvanced: Int = 0,
        val wordsReset: Int = 0,
        val wordsMastered: Int = 0,
        val failedWords: List<FailedWord> = emptyList(),
    )

    data class UiState(
        val phase: Phase = Phase.ANSWERING,
        val mode: Mode = Mode.REVIEW,
        val items: List<QuizItem> = emptyList(),
        val currentIndex: Int = 0,
        val selectedOptionIndex: Int? = null,
        val isCorrect: Boolean? = null,
        val didSkip: Boolean = false,
        val result: SessionResult = SessionResult(),
        val errorMessage: String? = null,
    ) {
        val currentItem: QuizItem? get() = items.getOrNull(currentIndex)
        val isLastQuestion: Boolean get() = currentIndex >= items.lastIndex
        val progressFraction: Float
            get() = if (items.isEmpty()) 0f else (currentIndex + 1).toFloat() / items.size
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun prepare(words: List<LearnedWord>, mode: Mode = Mode.REVIEW) {
        val items = words.mapNotNull { word ->
            val quiz = word.quizzes.randomOrNull() ?: return@mapNotNull null
            QuizItem(word, quiz)
        }.shuffled()
        _state.value = UiState(
            phase = Phase.ANSWERING,
            mode = mode,
            items = items,
            result = SessionResult(totalQuestions = items.size),
        )
    }

    suspend fun skipAnswer() {
        val current = _state.value
        val item = current.currentItem ?: return
        if (current.phase != Phase.ANSWERING) return
        val failed = current.result.failedWords.toMutableList()
        if (failed.none { it.word.id == item.word.id }) {
            failed += FailedWord(item.word, item.quiz.options[item.quiz.correctOptionIndex])
        }
        _state.update {
            it.copy(
                didSkip = true,
                selectedOptionIndex = null,
                isCorrect = false,
                phase = Phase.SHOWING_FEEDBACK,
                result = it.result.copy(
                    skippedCount = it.result.skippedCount + 1,
                    failedWords = failed,
                ),
            )
        }
        if (current.mode == Mode.REVIEW) {
            submit(item, wrongOptionIndex(item.quiz.correctOptionIndex))
        }
    }

    suspend fun selectAnswer(index: Int) {
        val current = _state.value
        val item = current.currentItem ?: return
        if (current.phase != Phase.ANSWERING) return
        val correct = index == item.quiz.correctOptionIndex
        sounds.play(correct)
        val failed = current.result.failedWords.toMutableList()
        if (!correct && failed.none { it.word.id == item.word.id }) {
            failed += FailedWord(item.word, item.quiz.options[item.quiz.correctOptionIndex])
        }
        _state.update {
            it.copy(
                selectedOptionIndex = index,
                isCorrect = correct,
                didSkip = false,
                phase = Phase.SHOWING_FEEDBACK,
                result = it.result.copy(
                    correctCount = it.result.correctCount + if (correct) 1 else 0,
                    wrongCount = it.result.wrongCount + if (correct) 0 else 1,
                    failedWords = failed,
                ),
            )
        }
        if (current.mode == Mode.REVIEW) {
            submit(item, index)
        }
    }

    suspend fun continueToNext() {
        val current = _state.value
        if (current.isLastQuestion) {
            if (current.mode == Mode.PRACTICE) {
                _state.update { it.copy(phase = Phase.SUMMARY) }
                return
            }
            try {
                val response = store.completeReviewSession()
                reviewRequests.recordReviewSessionCompleted()
                _state.update {
                    it.copy(
                        phase = Phase.SUMMARY,
                        result = it.result.copy(xpEarned = it.result.xpEarned + response.xpEarned),
                    )
                }
            } catch (error: Exception) {
                _state.update { it.copy(errorMessage = error.userMessage()) }
            }
        } else {
            val item = current.currentItem
            val updated = item?.let { store.word(it.word.id)?.let { word -> it.copy(word = word) } }
            _state.update {
                val items = it.items.toMutableList()
                if (updated != null) items[it.currentIndex] = updated
                it.copy(
                    items = items,
                    currentIndex = it.currentIndex + 1,
                    selectedOptionIndex = null,
                    isCorrect = null,
                    didSkip = false,
                    phase = Phase.ANSWERING,
                )
            }
        }
    }

    companion object {
        fun wrongOptionIndex(correctOptionIndex: Int): Int =
            if (correctOptionIndex == 0) 1 else 0
    }

    private suspend fun submit(item: QuizItem, index: Int) {
        try {
            val response = store.submitReviewAnswer(item.word, item.quiz, index)
            if (response.result.wordsMastered > 0) reviewRequests.recordWordMastered()
            _state.update {
                it.copy(
                    result = it.result.copy(
                        xpEarned = it.result.xpEarned + response.result.xpEarned,
                        wordsAdvanced = it.result.wordsAdvanced + response.result.wordsAdvanced,
                        wordsReset = it.result.wordsReset + response.result.wordsReset,
                        wordsMastered = it.result.wordsMastered + response.result.wordsMastered,
                    ),
                )
            }
        } catch (error: Exception) {
            _state.update { it.copy(errorMessage = error.userMessage()) }
        }
    }
}
