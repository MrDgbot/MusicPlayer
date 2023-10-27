package com.example.musicplayer.ui.detail

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.musicplayer.R
import com.example.musicplayer.common.AppDatabase
import com.example.musicplayer.common.MusicPlayerManager
import com.example.musicplayer.data.Music
import com.example.musicplayer.databinding.PlayerDetailBinding
import com.example.musicplayer.ui.base.BaseActivity
import kotlinx.coroutines.*

class PlayerDetail : BaseActivity() {


    private var music: Music? = null
    private var musicIndex = -1

    private var updateUIJob: Job? = null
    private val rotateAnimator: ObjectAnimator? by lazy {
        ObjectAnimator.ofFloat(
            binding.playerBox.albumArtContainer,
            "rotation",
            0f,
            360f
        )
    }

    private val musicDao by lazy { AppDatabase.getInstance(this).musicDao() }
    private var lastPlayedMusicId: Int? = null
    private lateinit var binding: PlayerDetailBinding
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isDataLoaded = false
    private var downloadedMusics: List<Music> = emptyList()
    private var musicPlayerManager: MusicPlayerManager = MusicPlayerManager.getInstance(coroutineScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        initToolbar()
        startRotationAnimation()
        setOnClickListeners()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        lastPlayedMusicId = getSharedPreferences("musicPlayerManager", Context.MODE_PRIVATE)
            .getInt("lastPlayedMusicId", -1)
        handleMusicIntent()
    }

    override fun onPause() {
        super.onPause()
        pauseMusic()
        getSharedPreferences("musicPlayerManager", Context.MODE_PRIVATE)
            .edit()
            .putInt("lastPlayedMusicId", lastPlayedMusicId ?: -1)
            .apply()
    }

    private fun initUI() {
        binding = PlayerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initToolbar() {
        val toolbar = binding.appbar.toolbar
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayShowTitleEnabled(false);
        binding.appbar.toolbar.setNavigationOnClickListener() {
            onBackPressed()
        }

    }

    /**
     * 处理获取的音乐。
     * 判断是需要恢复播放的还是需要播放新的音乐。
     * 如果是恢复播放，则调用continuePlaying()函数。
     * 如果是播放新的音乐，则调用updateMusicUI()函数更新音乐界面，然后调用playMusic()函数播放音乐。
     * @see getMusicFromIntent
     * @see isSameMusicPlaying
     * @see continuePlaying
     * @see updateMusicUI
     * @see playMusic
     */
    private fun handleMusicIntent() {
        music = getMusicFromIntent()
        music?.let {
            if (isSameMusicPlaying()) {
                continuePlaying()
            } else {
                music?.let { music ->
                    updateMusicUI()
                    playMusic(music)
                } ?: onError("数据加载出错, 请重试")

            }
        }
    }

    private fun getMusicFromIntent(): Music? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("music", Music::class.java)
        } else {
            intent.getParcelableExtra("music")
        }
    }

    private fun isSameMusicPlaying(): Boolean {
        Log.d("PlayerDetail", "isSameMusicPlaying: ${music?.id} $lastPlayedMusicId")
        return music?.id == lastPlayedMusicId
    }

    /**
     * 它用于在以下按钮上设置点击事件监听器：
     * - previousButton
     * - nextButton
     * - musicButton
     *
     * 当点击previousButton时，会调用playPrevious()函数播放上一首歌曲。
     * 当点击nextButton时，会调用playNext()函数播放下一首歌曲。
     * 当点击musicButton时，会调用toggleMusic()函数切换音乐的播放*/
    private fun setOnClickListeners() {
        binding.previousButton.setOnClickListener { playPrevious() }
        binding.nextButton.setOnClickListener { playNext() }
        binding.musicButton.setOnClickListener { toggleMusic() }
    }

    private fun playPrevious() {
        val previousMusic = getMusicByOffset(-1)
        if (previousMusic != null) {
            stopMusic()
            playMusic(previousMusic)
            switchMusicUIUpdate(previousMusic, false)
            resetUIUpdateJobState()
        }
    }

    private fun playNext() {
        val nextMusic = getMusicByOffset(1)
        if (nextMusic != null) {
            stopMusic()
            playMusic(nextMusic)
            switchMusicUIUpdate(nextMusic, true)
            resetUIUpdateJobState()
        }
    }

    /**
     * 根据偏移量获取音乐。
     *
     * @param offset 偏移量，它定义了要从当前播放索引开始寻找的音乐位置。负数表示后退，正数表示前进。
     *
     * @return 返回音乐对象。如果下载的音乐列表为空，或者没有找到偏移量指定的音乐，此方法将返回 null。如果索引超出范围，将通过Toast提示用户。
     */
    private fun getMusicByOffset(offset: Int): Music? {
        if (downloadedMusics.isEmpty()) {
            return null
        }
        if (musicIndex == -1) {
            musicIndex = downloadedMusics.indexOf(music)
        }
        musicIndex += offset
        if (musicIndex >= 0 && musicIndex < downloadedMusics.size) {
            return downloadedMusics[musicIndex]
        }
        if (musicIndex == -1) {
            showToast("前面没有歌曲了")
        }
        if (musicIndex == downloadedMusics.size) {
            showToast("这是最后一首咯")
        }
        musicIndex -= offset
        return null
    }

    private fun toggleMusic() {
        if (musicPlayerManager.isPlaying()) pauseMusic() else resumeMusic()
    }

    /**
     * 这是一个私有的函数`loadData()`. 这个函数主要用于在协程范围内启动一个新的协程, 并且获取已经下载的音乐.
     * 在这个函数执行完毕后, `isDataLoaded`会被置为`true`, 表明数据已经成功的加载了.
     *
     *
     */
    private fun loadData() {
        coroutineScope.launch {
            try {
                downloadedMusics = musicDao.getDownloadedMusics()
            } catch (e: Exception) {
                e.printStackTrace()
                onError("本地加载数据出错, 请重试")
            } finally {
                isDataLoaded = true
            }
        }
    }

    /**
     * 继续播放音乐的方法。
     *
     * 在暂停后，可以调用这个方法以恢复音乐播放。调用这个方法时，会执行以下操作：
     * 1. 输出当前音乐播放器的位置信息到日志。
     * 2. 恢复音乐播放。
     * 3. 更新音乐界面。
     * 4. 启动更新UI的任务。
     * 5. 恢复旋转动画。
     *
     */
    private fun continuePlaying() {
        Log.d("PlayerDetail", "continuePlaying${musicPlayerManager.getCurrentPosition()}")
        resumeMusic()
        updateMusicUI()
        startUpdateUIJob()
        resumeRotationAnimation()
    }

    private fun playMusic(music: Music) {
        rotatePointer(false)
        startRotationAnimation()
        musicPlayerManager.startMusic(this, music, onError = {
            Toast.makeText(this@PlayerDetail, it, Toast.LENGTH_SHORT).show()
            music.downloaded = false
            onError("播放器异常, 请重试")
        })
        musicPlayerManager.setOnCompletionListener {
            playNext()
        }
        val endTime = formatTime(musicPlayerManager.getDuration())
        binding.endTime.text = endTime
        binding.musicButton.setImageResource(R.drawable.pause)
        startUpdateUIJob()
    }

    private fun pauseMusic() {
        musicPlayerManager.pauseMusic()
        binding.musicButton.setImageResource(R.drawable.play)
        pauseRotationAnimation()
        rotatePointer()
    }

    private fun resumeMusic() {
        musicPlayerManager.resumeMusic()
        binding.musicButton.setImageResource(R.drawable.pause)
        resumeRotationAnimation()
        rotatePointer(false)
    }

    private fun stopMusic() {
        Log.d("PlayerDetail", "stopMusic")
        binding.musicButton.setImageResource(R.drawable.play)
        pauseRotationAnimation()
        rotatePointer()
        musicPlayerManager.stopMusic()
    }

    /**
     * 更新音乐界面的函数. 此函数将播放器的各个UI组件进行了初始化和配置.
     *
     * 这个函数首先更新上次播放的歌曲的ID。接着设置UI界面中的开始时间和结束时间为0，然后显示正在播放的歌曲的名字和艺术家的名字。
     * 另外，如果有封面图片，也会显示出来。接着设置播放者的背景图片，设置进度条的最大值为100，并设置进度条的监听器。
     *
     * 在进度条的监听器中，如果用户拖动进度条，就会更新显示的开始时间，并跳转到对应的播放位置。
     * 注意，如果用户拖动进度条到最后，将会切换到下一首歌曲。如果在播放过程中出现任何错误，会显示"播放出错"的提示，并关闭应用程序。
     */
    private fun updateMusicUI() {
        lastPlayedMusicId = music?.id
        binding.startTime.text = getString(R.string.zero_time)
        binding.endTime.text = getString(R.string.zero_time)
        binding.songName.text = music?.title
        binding.artistName.text = music?.artist
        setImage(music?.cover.orEmpty())

        binding.playerBox.playerBg.setImageResource(R.drawable.player_play_bg)
        binding.seekBar.max = 100
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    try {
                        val currentPosition = musicPlayerManager.getCurrentPosition()
                        val startTime = formatTime(currentPosition)
                        /// 如果拖到最后，就直接播放下一首
                        if (progress == 100) {
                            playNext()
                            return
                        }
                        binding.startTime.text = startTime
                        musicPlayerManager.seekTo(progress * musicPlayerManager.getDuration() / 100)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onError("页面seek播放出错")
                    }

                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * 这是 `startUpdateUIJob` 方法的文档.
     * 该方法负责开启一个更新UI的协程任务. 首先检查 `updateUIJob` 是否已经激活,
     * 如果已经激活, 将会取消这个任务. 然后在 `coroutineScope` 中启动新的更新UI的任务.
     *
     * 这个任务会持续检查音乐播放器 `musicPlayerManager` 是否正在播放音乐.
     * 如果正在播放, 会持续获取当前播放位置更新开始时间和进度条的进度.
     * 当发生错误时, 会捕获异常并打印错误堆栈信息, 同时弹出提示信息并结束当前活动.
     *
     * 任务每执行一次循环后会暂停一秒.
     *
     * 在任务完成时（不论是否因为错误而结束）, 会尝试停止旋转动画并改变播放按钮的图标.
     */
    private fun startUpdateUIJob() {
        if (updateUIJob?.isActive == false) {
            updateUIJob?.cancel()
        }
        updateUIJob = coroutineScope.launch {
            while (isActive) {

                if (musicPlayerManager.isPlaying()) {
                    try {
                        val currentPosition = musicPlayerManager.getCurrentPosition()
                        val startTime = formatTime(currentPosition)
                        binding.startTime.text = startTime
                        binding.seekBar.progress = currentPosition * 100 / musicPlayerManager.getDuration()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onError("页面播放出错")
                    }

                }

                delay(1000)
            }
        }
        updateUIJob?.invokeOnCompletion { throwable ->
            if (throwable != null && throwable !is CancellationException) {
                Toast.makeText(this@PlayerDetail, "播放出错", Toast.LENGTH_SHORT).show()
            } else {
                pauseRotationAnimation()
                binding.musicButton.setImageResource(R.drawable.play)
            }
        }
    }

    /**
     * `resetUIUpdateJobState` 方法的说明。
     *
     * 这是一种私有方法，负责重置 UI 更新作业的状态。
     * 通过检查 `updateUIJob` 的当前状态，此方法可能会启动一个新的 `updateUIJob`。
     *
     * 如果 `updateUIJob` 为 `null` 或者 `updateUIJob` 当前不活跃（`isActive` 为 `false`），那么这个方法会调用 `startUpdateUIJob()` 来重新启动 `updateUIJob`。
     * /
     */
    private fun resetUIUpdateJobState() {
        if (updateUIJob == null || updateUIJob?.isActive == false) {
            startUpdateUIJob()
        }
    }

    /**
     * 格式化播放时间
     *
     * @param time 一个整数，代表时间的毫秒数。必须是非负的。
     * @return 返回一个字符串，它表示的是参数time转换成的"MM:SS"形式的时间。
     * 例如，如果time是90000，则该函数返回"01:30"。
     */
    private fun formatTime(time: Int): String {
        val minutes = time / 1000 / 60
        val seconds = (time / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * 该函数用于切换音乐并更新用户界面。
     *
     * @param music 需要播放的音乐。
     * @param isNext 如果为真，则播放下一首音乐，否则播放上一首。
     *
     * 此函数*/
    private fun switchMusicUIUpdate(music: Music, isNext: Boolean) {
        val endTime = formatTime(musicPlayerManager.getDuration())
        binding.endTime.text = endTime

        switchAnimate(binding.playerBox.albumArtContainer, isNext, music.cover)

        binding.seekBar.progress = 0

        if (musicPlayerManager.isPlaying()) {
            binding.musicButton.setImageResource(R.drawable.pause)
        } else {
            startRotationAnimation()
            binding.musicButton.setImageResource(R.drawable.play)
        }
    }

    /**
     * 切换动画方法. 用于改变专辑封面的位置和透明度，从而实现切歌动画效果。
     *
     * @param albumArt 是一个 ImageView 对象，表示当前显示的专辑封面。
     * @param isNext 是一个布尔值，表示切换动画的方向。如果为真，则动画向右移动；如果为假，则动画向左移动。
     * @param cover 是一个可空的字符串，表示将要显示的音乐封面的URL。如果不提供，则会使用当前音乐的封面。
     *
     * 这个方法首先会创建一个动画效果，然后立刻执行这个动画。动画效果包括将专辑封面向指定方向移动一段距离，并逐渐变透明。动画持续时间为500毫秒。
     *
     * 在动画结束后，会执行 endAnimationAction 方法，将专辑封面恢复到原位，并显示新的封面。
     * /
     */
    private fun switchAnimate(albumArt: View, isNext: Boolean, cover: String? = music?.cover) {
        /// 复制一个一模一样的view便于我做添加到跟布局中然后做动画

        val animateAction = {
            albumArt.animate()
                .translationXBy(if (isNext) albumArt.width.toFloat() else -albumArt.width.toFloat())
                .alpha(0f)
                .setDuration(500)
                .withStartAction {
                    rotateAnimator?.pause()
                }
                .withEndAction {
                    endAnimationAction(albumArt, !isNext, cover)
                }
                .start()
        }

        animateAction()
    }

    private fun endAnimationAction(albumArt: View, isNext: Boolean, cover: String?) {
        albumArt.translationX = if (isNext) albumArt.width.toFloat() else -albumArt.width.toFloat()
        albumArt.alpha = 1f
        setImage(cover)

        albumArt.animate()
            .translationXBy(if (isNext) -albumArt.width.toFloat() else albumArt.width.toFloat())
            .alpha(1f)
            .setDuration(500)
            .withEndAction {
                rotateAnimator?.resume()
            }
            .start()
    }

    /**
     * 开始封面旋转动画
     *
     * 动画的执行时间设定为2000毫秒（`rotateAnimator?.duration = 2000`）。
     * 该方法使用线性插值器（`LinearInterpolator()`）作为动画插值器。
     * 动画的重复次数设定为无限次（`ValueAnimator.INFINITE`）。
     * 最后，调用`start()`来开始动画。
     */
    private fun startRotationAnimation() {
        if (rotateAnimator?.isPaused == true) {
            resumeRotationAnimation()
            return
        }

        rotateAnimator?.duration = 2000
        rotateAnimator?.interpolator = LinearInterpolator()
        rotateAnimator?.repeatCount = ValueAnimator.INFINITE
        rotateAnimator?.start()
    }

    /*
    * 暂停封面旋转动画
    *
    * */
    private fun pauseRotationAnimation() {
        if (rotateAnimator?.isRunning == true) {
            Log.d("PlayerDetail", "pauseRotationAnimationS${rotateAnimator?.isPaused}")
            rotateAnimator?.pause()
            Log.d("PlayerDetail", "pauseRotationAnimationE${rotateAnimator?.isPaused}")
        }

    }

    /*
    * 恢复封面旋转动画
    * */
    private fun resumeRotationAnimation() {
        rotateAnimator?.resume()
    }


    /**
     * 根据提供的 Drawable 生成背景渐变色。
     *
     * @param drawable 可以为空。如果为空，则此函数不执行任何操作。
     * 此函数会首先设定 layout 背景色为白色。然后找到 layout 中的 ImageView， 并将它的高度设置为 layout 高度的一半。
     * 若 drawable 参数不为空，则通过 palette 提取 drawable 的主要颜色（即在图像中最广泛使用的颜色）。
     * 之后，根据主要颜色的亮度决定渐变的最终颜色，亮度大于0.7的颜色将与黑色混合，亮度不足0.7的颜色将与白色混合。
     * 用所得渐变色生成一个新的 GradientDrawable，并将 GradientDrawable 设置为 ImageView 的背景。
     *
     */
    private fun generateGradient(drawable: Drawable?) {
        val layout = binding.root
        layout.setBackgroundColor(Color.WHITE)

        val gradientImageView: ImageView = layout.findViewById(R.id.gradient_image_view)
        val layoutParams = gradientImageView.layoutParams
        layoutParams.height = layout.height / 2
        gradientImageView.layoutParams = layoutParams

        if (drawable == null) {
            return
        }

        val bitmap = drawable.toBitmap()
        val palette = Palette.from(bitmap).generate()
        val dominantColor = palette.dominantSwatch?.rgb ?: Color.WHITE

        Log.d("PlayerDetail", "dominant color: $dominantColor")
        var dominantColorEnd = dominantColor

        Log.d("PlayerDetail", "luminance: ${ColorUtils.calculateLuminance(dominantColor)}")
        dominantColorEnd = if (ColorUtils.calculateLuminance(dominantColor) > 0.7) {
            ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.2f)
        } else {
            ColorUtils.blendARGB(dominantColor, Color.WHITE, 0.4f)
        }

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.TRANSPARENT, Color.WHITE, dominantColorEnd)
        )

        Log.d("PlayerDetail", "gradient colors: ${gradientDrawable.colors.contentToString()}")

        gradientImageView.background = gradientDrawable
    }

    private fun setImage(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }

        Glide.with(this@PlayerDetail).load(url).circleCrop().listener(object : RequestListener<Drawable?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable?>?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable?>?,
                dataSource: com.bumptech.glide.load.DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                generateGradient(resource)
                return false
            }
        }).into(binding.playerBox.albumArt)
    }

    private fun rotatePointer(isPlaying: Boolean = true) {
        Log.d("PlayerDetail", "rotatePointer$isPlaying")
        if (isPlaying) {
            binding.playerBox.playerPointer.resetRotationWithAnimation()
        } else {
            binding.playerBox.playerPointer.rotateWithAnimation(6f)
        }
        val bgDrawable = if (isPlaying) R.drawable.player_play_bg else R.drawable.player_pause_bg
        binding.playerBox.playerBg.setImageResource(bgDrawable)
    }

    private fun onError(msg: String) {
        Toast.makeText(this, "播放出错", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        rotateAnimator?.cancel()
        updateUIJob?.cancel()
    }
}