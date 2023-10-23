package com.example.musicplayer.data

data class MusicResponse(
    val schemaVersion: String,
    val tracks: List<Music>
)