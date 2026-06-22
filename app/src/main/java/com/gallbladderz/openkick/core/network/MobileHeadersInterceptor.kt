package com.gallbladderz.openkick.core.network

import okhttp3.Interceptor
import okhttp3.Response

class MobileHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("User-Agent", KickApiConstants.USER_AGENT)
            .header("X-App-Platform", KickApiConstants.HEADER_PLATFORM)
            .header("X-App-Version", KickApiConstants.APP_VERSION)
            .header("X-Kick-App", "mobile")
            .header("X-Kick-App", KickApiConstants.HEADER_APP)
            .build()
        return chain.proceed(requestWithHeaders)
    }
}
