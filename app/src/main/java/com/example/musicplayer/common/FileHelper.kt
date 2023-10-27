package com.example.musicplayer.common

import android.content.Context
import com.example.musicplayer.data.Music
import java.io.File


class FileHelper private constructor(context: Context) {
    private val downloadDir: File by lazy { File(appContext.externalCacheDir, "music") }


    private val appContext: Context = context.applicationContext

    companion object {
        @Volatile
        private var instance: FileHelper? = null

        fun getInstance(context: Context): FileHelper {
            return instance ?: synchronized(this) {
                instance ?: FileHelper(context).also { instance = it }
            }
        }
    }

    // 创建文件
    fun createFile(filePath: String): Boolean {
        val file = File(filePath)

        if (file.exists()) {
            // 文件已存在
            return false
        }

        return try {
            // 创建文件
            file.createNewFile()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 判断文件是否存在
    fun isFileExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    // 获取下载目录
    fun getDir(): File {
        return downloadDir
    }

    // 获取下载文件
    fun getDownloadFile(music: Music): File {
        return File(downloadDir, "${music.id}_${music.title}.mp3")
    }


}
