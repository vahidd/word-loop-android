package com.codewiz.wordloop

import com.codewiz.wordloop.ui.theme.normalizeIosFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatTest {
    @Test
    fun convertsIosPlaceholdersForJavaFormatter() {
        val template = normalizeIosFormat("How is your %@?")
        assertThat(template).isEqualTo("How is your %s?")
        assertThat(template.format("English")).isEqualTo("How is your English?")
        assertThat(normalizeIosFormat("%lld word(s) waiting").format(13))
            .isEqualTo("13 word(s) waiting")
    }
}
