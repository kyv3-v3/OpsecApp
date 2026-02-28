package com.opsecapp.network.http

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkClientFactory {
  fun create(debug: Boolean): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
      level = if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    return OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .callTimeout(30, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .addInterceptor(HttpsOnlyInterceptor())
      .addInterceptor(logging)
      .build()
  }
}
