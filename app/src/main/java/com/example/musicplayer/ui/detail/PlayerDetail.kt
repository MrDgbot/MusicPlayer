package com.example.musicplayer.ui.detail

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.common.MusicPlayerManager
import com.example.musicplayer.data.Music
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class PlayerDetail : AppCompatActivity(), CoroutineScope {
    private var duration = 0
    private var music: Music? = null

    // musicPlayerManager 需要传递一个 CoroutineScope 对象

    private val job = Job()

    private val musicPlayerManager = MusicPlayerManager.getInstance(this)

    var rotateAnimator: ObjectAnimator? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_detail)

        startRotationAnimation()

        val seekBar = findViewById<SeekBar>(R.id.seek_bar)
        val albumArt = findViewById<ImageView>(R.id.album_art)
        /// 获取传递过来的Parcelable
        music = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("music", Music::class.java)
        } else {
            intent.getParcelableExtra("music")
        }

        if (music != null) {
            Glide.with(this)
                .load(music?.cover)
                .circleCrop()
                .into(albumArt)


            musicPlayerManager.startMusic(this, music!!)
            duration = musicPlayerManager.getDuration()

            val updateSeekBar = launch {
                while (musicPlayerManager.isPlaying()) {
                    seekBar.progress = musicPlayerManager.getCurrentPosition() * 100 / duration
                    delay(1000)   // 延迟1秒
                }
            }

            seekBar.max = 100
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        musicPlayerManager.seekTo(progress * duration / 100)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            updateSeekBar.invokeOnCompletion { throwable ->
                if (throwable != null && throwable !is CancellationException) {
                    Toast.makeText(this@PlayerDetail, "播放出错", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val playPauseButton = findViewById<ImageButton>(R.id.play_pause_button)
        playPauseButton.setOnClickListener {
            if (musicPlayerManager.isPlaying()) {
                musicPlayerManager.pauseMusic()
                playPauseButton.setImageResource(R.drawable.play)
                pauseRotationAnimation()
            } else {
                musicPlayerManager.resumeMusic()
                playPauseButton.setImageResource(R.drawable.pause)
                resumeRotationAnimation()
            }
        }


    }

    fun startRotationAnimation() {
        val albumArt = findViewById<ImageView>(R.id.album_art)

        rotateAnimator = ObjectAnimator.ofFloat(albumArt, "rotation", 0f, 360f)
        rotateAnimator?.duration = 3000
        rotateAnimator?.interpolator = LinearInterpolator()
        rotateAnimator?.repeatCount = ValueAnimator.INFINITE
        rotateAnimator?.start()
    }

    // 暂停动画的函数
    fun pauseRotationAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            rotateAnimator?.pause()
        } else {
            rotateAnimator?.cancel()
        }
    }

    // 恢复动画的函数
    fun resumeRotationAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            rotateAnimator?.resume()
        } else {
            rotateAnimator?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}