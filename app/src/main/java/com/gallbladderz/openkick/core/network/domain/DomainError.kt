package com.gallbladderz.openkick.core.domain

sealed class DomainError(message: String) : Exception(message) {
    class NetworkError(message: String = "Network connection failed") : DomainError(message)
    class OfflineError(message: String = "Streamer is currently offline") : DomainError(message)
    class UnknownError(message: String = "An unknown error occurred") : DomainError(message)
    class ApiError(message: String) : DomainError(message)
}
