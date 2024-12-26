package com.mysticgsi.dsu.networking

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET()
    fun getFirmwareList(@Url url: String): Call<FirmwareResponse>

    @GET("dsuversion.brick")
    fun getVersion(): Call<VersionResponse>
}

object ApiClient {
    private const val BASE_URL = "https://mysticcloudmain.hpdevfox.ru/"

    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request: Request = chain.request().newBuilder()
                .addHeader("User-Agent", "MysticDSU/1.1 (Linux meow; ${Build.VERSION.CODENAME}; Android ${Build.VERSION.RELEASE}; ${Build.PRODUCT} Build/brick)")
                .build()
            chain.proceed(request)
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}