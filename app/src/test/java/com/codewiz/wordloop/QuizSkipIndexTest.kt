package com.codewiz.wordloop

import com.codewiz.wordloop.ui.quiz.QuizViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuizSkipIndexTest {
    @Test
    fun skipUsesAWrongOptionIndex() {
        assertThat(QuizViewModel.wrongOptionIndex(0)).isEqualTo(1)
        assertThat(QuizViewModel.wrongOptionIndex(1)).isEqualTo(0)
        assertThat(QuizViewModel.wrongOptionIndex(3)).isEqualTo(0)
        assertThat(QuizViewModel.wrongOptionIndex(0)).isNotEqualTo(0)
    }

    @Test
    fun summaryTitleFollowsAccuracyBands() {
        assertThat(QuizViewModel.resultTitle(4, 5)).isEqualTo("Excellent!")
        assertThat(QuizViewModel.resultTitle(3, 5)).isEqualTo("Good job!")
        assertThat(QuizViewModel.resultTitle(1, 5)).isEqualTo("Keep practicing!")
        assertThat(QuizViewModel.resultTitle(0, 0)).isEqualTo("Keep practicing!")
    }
}
