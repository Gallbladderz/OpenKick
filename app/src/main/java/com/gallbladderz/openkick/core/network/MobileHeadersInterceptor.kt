package com.gallbladderz.openkick.core.network

import okhttp3.Interceptor
import okhttp3.Response

class MobileHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("User-Agent", KickApiConstants.USER_AGENT)
            .header("X-App-Platform", "Android")
            .header("X-App-Version", "40.21.0")
            .header("X-Kick-App", "mobile")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(requestWithHeaders)
    }
}
