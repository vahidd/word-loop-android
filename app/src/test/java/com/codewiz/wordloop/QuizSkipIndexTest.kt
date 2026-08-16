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
}
