package com.aurachat.domain.error

/**
 * Domain-layer error hierarchy for AuraChat.
 *
 * Provides typed error handling across the application, allowing ViewModels and use cases
 * to distinguish between different failure modes (network issues, API errors, database
 * failures, validation errors) and present appropriate error messages to users.
 */
sealed class DomainError : Exception() {
    /**
     * Network connectivity errors (no internet, timeout, DNS failure).
     *
     * @property message User-friendly error description
     */
    data class NetworkError(override val message: String) : DomainError()

    /**
     * AI service API errors with HTTP status codes.
     *
     * @property code The HTTP status code (e.g., 429 for rate limit, 500 for server error)
     * @property message User-friendly error description
     */
    data class ApiError(val code: Int, override val message: String) : DomainError()

    /**
     * Local database operation failures (Room errors, constraint violations).
     *
     * @property message User-friendly error description
     */
    data class DatabaseError(override val message: String) : DomainError()

    /**
     * Input validation errors (empty messages, blank titles).
     *
     * @property message User-friendly error description
     */
    data class ValidationError(override val message: String) : DomainError()

    /**
     * Fallback for unexpected errors that don't fit other categories.
     *
     * @property message User-friendly error description
     */
    data object UnknownError : DomainError() {
        override val message: String = "An unknown error occurred"
    }
}
