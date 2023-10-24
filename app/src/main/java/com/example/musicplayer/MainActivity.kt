package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateToMusicList()
    }

    private fun navigateToMusicList() {
        val intent = Intent(this, MusicListActivity::class.java)
        startActivity(intent)
    }
}