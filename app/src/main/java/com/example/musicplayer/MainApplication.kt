package com.example.musicplayer

import android.app.Application
import com.example.musicplayer.common.AppDatabase
import com.example.musicplayer.common.DownloadManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainApplication : Application() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
        // 重置所有下载任务(协程)
        GlobalScope.launch {
            DownloadManager.resetAllDownloads(this@MainApplication)
        }

    }
}

