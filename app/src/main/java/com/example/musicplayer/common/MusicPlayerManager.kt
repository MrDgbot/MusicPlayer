package com.example.musicplayer.common

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.musicplayer.dao.MusicDao
import com.example.musicplayer.data.Music
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicPlayerManager private constructor(private val coroutineScope: CoroutineScope) {

    companion object {
        private var instance: MusicPlayerManager? = null

        fun getInstance(coroutineScope: CoroutineScope): MusicPlayerManager {
            if (instance == null) {
                instance = MusicPlayerManager(coroutineScope)
            }
            return instance as MusicPlayerManager
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null
    private lateinit var musicDao: MusicDao


    fun startMusic(context: Context, music: Music, onError: (String) -> Unit) {
        musicDao = AppDatabase.getInstance(context).musicDao()

        if (mediaPlayer != null) {
            mediaPlayer?.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }

        val downloadDir = File(context.externalCacheDir, "music")
        val file = File(downloadDir, "${music.id}_${music.title}.mp3")

        mediaPlayer?.setDataSource(context, Uri.fromFile(file))
        mediaPlayer?.prepare()

        mediaPlayer?.setOnCompletionListener {
            it.release()
            mediaPlayer = null
            currentMusic = null
        }

        mediaPlayer?.seekTo(music.currentPosition)
        mediaPlayer?.start()
        currentMusic = music
        currentMusic?.playing = true

        coroutineScope.launch {
            updateMusic(currentMusic!!)
        }
    }

    fun pauseMusic() {
        if (isPlaying()) {
            mediaPlayer?.pause()
            currentMusic?.playing = false

            // 更新数据库中的播放状态和播放位置
            coroutineScope.launch {
                updateMusic(currentMusic!!)
            }
        }
    }

    fun resumeMusic() {
        if (!isPlaying()) {
            mediaPlayer?.start()
            currentMusic?.playing = true // 更新播放状态
        }
        coroutineScope.launch {
            currentMusic?.let { updateMusic(it) }
        }
    }

    fun stopMusic() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusic = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentMusic(): Music? {
        return currentMusic
    }

    private suspend fun updateMusic(music: Music) {
        withContext(Dispatchers.IO) {
            musicDao.update(music)
        }
    }

    /// 播放完毕
    fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener) {
        mediaPlayer?.setOnCompletionListener(listener)
    }
}