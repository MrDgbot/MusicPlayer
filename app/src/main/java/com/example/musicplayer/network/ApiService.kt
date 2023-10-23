package com.example.musicplayer.network

import com.example.musicplayer.data.MusicResponse
import retrofit2.http.GET

interface ApiService {
    @GET("music-list/-/raw/main/music-list.json")
    suspend fun getTracks(): MusicResponse
}