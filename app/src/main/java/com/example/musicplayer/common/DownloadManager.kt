package com.example.musicplayer.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import com.example.musicplayer.data.Music
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// 下载管理器，用于管理所有的下载任务
object DownloadManager {
    // 使用线程安全的HashMap来存储和管理下载任务
    private val tasks: MutableMap<String, DownloadTask> = ConcurrentHashMap()


    // 开始下载音乐文件
    fun startDownload(
        context: Context,
        music: Music,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (!isNetworkConnected(context)) {
            onError("无网络连接")
            return
        }

        if (tasks.containsKey(music.url)) {
            return
        }

        val downloadDir = File(context.externalCacheDir, "music")
        val musicDao = AppDatabase.getInstance(context).musicDao()

        val task = DownloadTask(music, downloadDir, onProgress, onError, onSuccess, musicDao)
        tasks[music.url] = task
        task.start()
    }

    // 取消下载任务
    suspend fun cancelDownload(url: String) {
        tasks[url]?.cancel()
        tasks.remove(url)
    }

    // 取消所有下载任务
    suspend fun cancelAll() {
        // 取消所有的下载任务并清空列表
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

    suspend fun resetAllDownloads(context: Context) {
        val musicDao = AppDatabase.getInstance(context).musicDao()
        musicDao.getAllMusics().forEach { song ->
            if (song.downloading) {
                song.downloading = false
                song.localDownloadSize = 0
                musicDao.update(song)
            }
            musicDao.update(song)
        }
    }

    // 在DownloadTask类中添加一个检查网络连接状态的方法

    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (SDK_INT >= VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

}
