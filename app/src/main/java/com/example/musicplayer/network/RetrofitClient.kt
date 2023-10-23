package com.example.musicplayer.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val retrofit: Retrofit

    init {
        val client = OkHttpClient.Builder()
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("https://gitlab.com/michaelins.shi/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getRetrofit(): Retrofit {
        return retrofit
    }
}