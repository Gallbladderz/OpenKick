package com.gallbladderz.openkick.core.domain

import retrofit2.HttpException
import java.io.IOException

sealed class DomainError(message: String) : Exception(message) {
    class NetworkError(message: String = "Network connection failed") : DomainError(message)
    class OfflineError(message: String = "Streamer is currently offline") : DomainError(message)
    class UnknownError(message: String = "An unknown error occurred") : DomainError(message)
    class ApiError(message: String) : DomainError(message)
}

fun Throwable.toDomainError(): DomainError {
    return when (this) {
        is DomainError -> this
        is HttpException -> DomainError.ApiError("HTTP Error: ${this.code()}")
        is IOException -> DomainError.NetworkError()
        else -> DomainError.UnknownError(this.message ?: "An unknown error occurred")
    }
}
