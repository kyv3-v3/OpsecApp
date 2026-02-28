package com.opsecapp.network.http

import okhttp3.Interceptor
import okhttp3.Response

class HttpsOnlyInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    check(request.url.isHttps) {
      "Cleartext HTTP blocked for ${request.url}"
    }
    return chain.proceed(request)
  }
}
