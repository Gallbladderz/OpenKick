package com.gallbladderz.openkick.features.home

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HomeRepository(private val client: HttpClient) {
    fun fetchTestStream(): Flow<Result<String>> = flow {
        try {
            val response = client.get("v1/channels/xqc").bodyAsText()
            emit(Result.success(response.take(500)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}