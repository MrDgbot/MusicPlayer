package com.example.musicplayer.ui.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.common.AppDatabase
import com.example.musicplayer.common.DownloadManager
import com.example.musicplayer.dao.MusicDao
import com.example.musicplayer.data.Music
import com.example.musicplayer.ui.detail.PlayerDetail
import kotlinx.coroutines.*

class MusicListAdapter : ListAdapter<Music, MusicListAdapter.ViewHolder>(MusicDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = currentList[position]
        holder.bind(track)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        private val artistTextView: TextView = itemView.findViewById(R.id.artist_text_view)
        private val musicCover: ImageView = itemView.findViewById(R.id.music_cover)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val actionButton: Button = itemView.findViewById(R.id.action_button)
        private val musicDao: MusicDao = AppDatabase.getInstance(itemView.context).musicDao()

        private lateinit var track: Music

        init {
            actionButton.setOnClickListener {
                onDownloadButtonClick()
            }
        }

        /**
         * 将给定的音乐绑定到当前视图。
         *
         * @param track 要绑定的音乐对象
         */
        fun bind(track: Music) {
            this.track = track

            titleTextView.text = track.title
            artistTextView.text = track.artist
            updateImageView()
            updateDownloadButtonVisibility()
            updateDownloadButtonText()
            updateProgress()
        }

        /**
         * 当点击下载按钮时的方法。
         * 如果音轨已经下载完成，跳转到播放页面。
         * 如果音轨正在下载中，取消下载。
         * 如果音轨未下载，开始下载。
         *
         */
        private fun onDownloadButtonClick() {
            if (track.downloaded) {
                // 跳转到播放页面
                startPlayerDetailActivity(itemView.context, track)
                return
            }
            if (track.downloading) {
                cancelDownload()
            } else {
                startDownload()
            }
        }

        /**
         * 启动播放器详细页面的方法
         *
         * @param context 上下文对象
         * @param Music 音乐对象
         */
        private fun startPlayerDetailActivity(context: Context, music: Music) {
            val intent = Intent(context, PlayerDetail::class.java)
            intent.putExtra("music", music)

            context.startActivity(intent)
        }

        /**
         * 开始下载。
         * 此方法用于开始下载操作。
         * 它会调用 DownloadManager 的 startDownload 方法来启动下载，并通过传入的参数设置相应的回调函数和 UI 更新。
         * 下载过程中会更新进度条，下载完成后会更新下载按钮的文本。
         * 同时，还会将进度条设置为可见，并将下载按钮的文本设置为取消。
         *
         */
        private fun startDownload() {
            DownloadManager.startDownload(itemView.context, track,
                onProgress = { progressBar.progress = it },
                onError = {
                    Toast.makeText(itemView.context, it, Toast.LENGTH_SHORT).show()
                    updateDownloadButtonText()
                },
                onSuccess = {
                    updateDownloadButtonText()
                    updateProgress()
                }
            )
            progressBar.visibility = View.VISIBLE
            actionButton.text = itemView.context.getString(R.string.cancel)
        }

        /**
         * 取消下载方法
         *
         * 该方法用于取消正在进行中的下载操作。
         *
         * 在后台线程中启动一个协程，使用IO调度器进行操作。
         * 调用DownloadManager.cancelDownload(track.url)来取消下载。
         * 然后切换到主线程，使用Main调度器进行操作。
         * 首先通过musicDao.getMusicById(track.id)获取最新的音乐对象，
         * 如果返回为空，则使用当前的track对象。
         * 修改下载按钮文本为"下载"。
         * 将进度条的可见性设置为不可见，进度设置为0。
         */
        private fun cancelDownload() {
            CoroutineScope(Dispatchers.IO).launch {
                DownloadManager.cancelDownload(track.url)
                withContext(Dispatchers.Main) {
                    track = musicDao.getMusicById(track.id) ?: track

                    actionButton.text = itemView.context.getString(R.string.download)
                    progressBar.visibility = View.INVISIBLE
                    progressBar.progress = 0
                }
            }
        }


        /**
         * 更新图片视图。
         *
         * 该方法使用Glide库加载指定图片资源，并将加载的图片设置到目标ImageView。
         * 使用圆角转换器对图片进行圆角处理，圆角大小为4像素。
         * 使用磁盘缓存策略缓存所有图片。
         * 如果加载过程中出现错误，则会显示占位图和错误图。
         *
         */
        private fun updateImageView() {
            Glide.with(itemView)
                .load(track.cover)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(4)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_background)
                .into(musicCover)
        }

        /**
         * 更新下载按钮的可见性
         *
         * 此方法用于根据当前下载状态更新下载按钮的显示状态。
         * 如果正在下载中，下载按钮将可见；如果未在下载，则下载按钮将不可见。
         */
        private fun updateDownloadButtonVisibility() {
            progressBar.visibility = if (track.downloading) View.VISIBLE else View.INVISIBLE
        }

        /**
         * 更新下载按钮的文本。
         *
         * 根据当前歌曲的状态，更新下载按钮的文本显示。如果歌曲已下载完成，则显示"播放"；
         * 如果歌曲正在下载中，则显示"取消"；如果歌曲未下载，则显示"下载"。
         *
         */
        private fun updateDownloadButtonText() {
            actionButton.text = if (track.downloaded) {
                itemView.context.getString(R.string.play)
            } else if (track.downloading) {
                itemView.context.getString(R.string.cancel)
            } else {
                itemView.context.getString(R.string.download)
            }
        }

        /**
         * 更新进度的方法。
         * 如果正在下载音乐，则计算并显示下载进度。
         * 如果音乐已下载完成，则隐藏进度条。
         */
        private fun updateProgress() {
            if (track.downloading) {
                CoroutineScope(Dispatchers.IO).launch {
                    val downloadSize = track.localDownloadSize
                    val downloadTotalSize = track.downloadTotalSize
                    if (downloadSize == 0L) {
                        return@launch
                    }
                    val progress = (downloadSize * 100 / downloadTotalSize).toInt()
                    withContext(Dispatchers.Main) {
                        progressBar.progress = progress
                    }
                }
            }
            if (track.downloaded) {
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * MusicDiffCallback是一个用于DiffUtil的回调类，
     * 用于比较两个音乐实例是否代表同一个音乐项以及两个音乐实例的内容是否相同。
     *
     */
    private class MusicDiffCallback : DiffUtil.ItemCallback<Music>() {
        override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem == newItem
        }
    }
}