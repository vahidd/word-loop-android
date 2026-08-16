package com.codewiz.wordloop.data.api

sealed class ApiError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data object InvalidUrl : ApiError("Invalid server URL. Check your backend URL in Settings.")
    data object Unauthorized : ApiError("You must be signed in to use this feature.")
    data class Network(val detail: String) : ApiError("Network error: $detail")
    data class Decoding(val detail: String) : ApiError("Could not parse server response.")
    data class Validation(val detail: String) : ApiError(detail)
    data object RateLimited : ApiError("Too many requests. Please try again in a minute.")
    data class AiGenerationFailed(val detail: String) : ApiError(detail)
    data class WordNotRecognized(
        val detail: String,
        val suggestion: String?,
    ) : ApiError(detail)
    data class Server(val code: String, val detail: String) : ApiError(detail)
    data class UnexpectedStatus(val code: Int) : ApiError("Unexpected server response ($code).")

    val isRetryable: Boolean
        get() = this is RateLimited || this is AiGenerationFailed || this is Network

    companion object {
        fun fromBody(status: Int, body: ApiErrorBody?): ApiError {
            val detail = body?.error
            return when (status) {
                401 -> Unauthorized
                409 -> Server(detail?.code.orEmpty(), detail?.message ?: "Conflict")
                422 -> if (detail?.code == "WORD_NOT_RECOGNIZED") {
                    WordNotRecognized(
                        detail = detail.message,
                        suggestion = detail.suggestion,
                    )
                } else {
                    Server(detail?.code.orEmpty(), detail?.message ?: "Unprocessable request")
                }
                429 -> RateLimited
                400 -> Validation(detail?.message ?: "Invalid request")
                502 -> AiGenerationFailed(detail?.message ?: "Could not complete the request.")
                else -> if (detail != null) {
                    Server(detail.code, detail.message)
                } else {
                    UnexpectedStatus(status)
                }
            }
        }
    }
}
