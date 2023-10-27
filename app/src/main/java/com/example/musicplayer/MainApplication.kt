package com.example.musicplayer

import android.app.Application
import com.example.musicplayer.common.AppDatabase
import com.example.musicplayer.common.DownloadManager
import com.example.musicplayer.common.FileHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainApplication : Application() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        AppDatabase.getInstance(this)

        FileHelper.getInstance(this)

        // 清理历史所有下载状态
        GlobalScope.launch {
            DownloadManager.resetAllDownloads(this@MainApplication)
        }

    }
}

