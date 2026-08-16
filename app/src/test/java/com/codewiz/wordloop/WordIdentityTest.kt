package com.codewiz.wordloop

import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.util.wordInputFormatted
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WordIdentityTest {
    @Test
    fun wordIdentityIsCaseSensitive() {
        val morning = LearnedWord(id = "1", word = "Morgen", language = "German")
        val tomorrow = LearnedWord(id = "2", word = "morgen", language = "German")
        assertThat(morning.matchesWordIdentity("Morgen", "German")).isTrue()
        assertThat(morning.matchesWordIdentity("morgen", "German")).isFalse()
        assertThat(tomorrow.matchesWordIdentity("morgen", "German")).isTrue()
        assertThat(morning.matchesWordIdentity("Morgen", "English")).isFalse()
    }

    @Test
    fun wordInputTrimsButPreservesCase() {
        assertThat("  Morgen  ".wordInputFormatted()).isEqualTo("Morgen")
        assertThat("morgen".wordInputFormatted()).isEqualTo("morgen")
    }
}
