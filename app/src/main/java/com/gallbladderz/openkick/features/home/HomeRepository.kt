package com.gallbladderz.openkick.features.home

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HomeRepository(private val client: HttpClient) {
    fun fetchTestStream(): Flow<Result<KickChannelResponse>> = flow {
        try {
            val response = client.get("v1/channels/gladvalakaspwnz").body<KickChannelResponse>()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}