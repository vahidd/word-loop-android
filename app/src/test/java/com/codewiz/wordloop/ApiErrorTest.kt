package com.codewiz.wordloop

import com.codewiz.wordloop.data.api.ApiError
import com.codewiz.wordloop.data.api.ApiErrorBody
import com.codewiz.wordloop.data.api.ApiErrorDetail
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiErrorTest {
    @Test
    fun wordNotRecognizedIncludesSuggestion() {
        val error = ApiError.fromBody(
            422,
            ApiErrorBody(
                error = ApiErrorDetail(
                    code = "WORD_NOT_RECOGNIZED",
                    message = "We couldn't find \"helo\" in English.",
                    suggestion = "hello",
                ),
            ),
        )
        assertThat(error).isInstanceOf(ApiError.WordNotRecognized::class.java)
        val typed = error as ApiError.WordNotRecognized
        assertThat(typed.suggestion).isEqualTo("hello")
        assertThat(typed.message).contains("helo")
    }

    @Test
    fun conflictMapsToServerError() {
        val error = ApiError.fromBody(
            409,
            ApiErrorBody(error = ApiErrorDetail(code = "WORD_ALREADY_EXISTS", message = "exists")),
        )
        assertThat(error).isInstanceOf(ApiError.Server::class.java)
        assertThat((error as ApiError.Server).code).isEqualTo("WORD_ALREADY_EXISTS")
    }

    @Test
    fun unauthorizedAndRateLimit() {
        assertThat(ApiError.fromBody(401, null)).isEqualTo(ApiError.Unauthorized)
        assertThat(ApiError.fromBody(429, null)).isEqualTo(ApiError.RateLimited)
        assertThat(ApiError.RateLimited.isRetryable).isTrue()
        assertThat(ApiError.Unauthorized.isRetryable).isFalse()
    }

    @Test
    fun aiFailureIsRetryable() {
        val error = ApiError.fromBody(502, ApiErrorBody(error = ApiErrorDetail(message = "down")))
        assertThat(error).isInstanceOf(ApiError.AiGenerationFailed::class.java)
        assertThat(error.isRetryable).isTrue()
    }
}
