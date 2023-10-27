package com.example.musicplayer.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import com.example.musicplayer.data.Music
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

        val musicDao = AppDatabase.getInstance(context).musicDao()

        val task =
            DownloadTask(music, FileHelper.getInstance(context).getDir(), onProgress, onError, onSuccess, musicDao)
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
                musicDao.update(song)
            }
            musicDao.update(song)
        }
    }

    // 删除整个下载目录
    suspend fun deleteDownloadDir(context: Context) {
        val musicDao = AppDatabase.getInstance(context).musicDao()
        musicDao.getAllMusics().forEach { song ->
            if (song.downloading) {
                song.downloading = false
                musicDao.update(song)
            }
            musicDao.update(song)
        }
        FileHelper.getInstance(context).getDir().deleteRecursively()
    }

    /**
     * 检查设备是否连接到网络。
     *
     * @param context 应用程序的上下文。通常传递`getApplicationContext()`或`this`（对于Activity或Fragment的实例）。
     * @return 如果设备连接到网络（移动数据或Wi-Fi），则返回true；否则返回false。
     *
     */
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
