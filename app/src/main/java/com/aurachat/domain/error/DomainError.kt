package com.aurachat.domain.error

sealed class DomainError : Exception() {
    data class NetworkError(override val message: String) : DomainError()
    data class ApiError(val code: Int, override val message: String) : DomainError()
    data class DatabaseError(override val message: String) : DomainError()
    data class ValidationError(override val message: String) : DomainError()
    data object UnknownError : DomainError() {
        override val message: String = "An unknown error occurred"
    }
}
