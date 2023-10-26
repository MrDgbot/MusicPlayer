package com.example.musicplayer.network

import com.example.musicplayer.data.MusicResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {
    @GET("music-list/-/raw/main/music-list.json")
    suspend fun getTracks(): MusicResponse

    @Streaming
    @GET
    suspend fun downloadFileWithDynamicUrlAsync(
        @Url fileUrl: String,
        @Header("Range") range: String? = null
    ): Response<ResponseBody>
}