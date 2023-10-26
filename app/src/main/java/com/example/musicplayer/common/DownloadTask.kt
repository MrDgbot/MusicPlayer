package com.example.musicplayer.common


import com.example.musicplayer.dao.MusicDao
import com.example.musicplayer.data.Music
import com.example.musicplayer.network.ApiService
import com.example.musicplayer.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import retrofit2.Response
import java.io.File
import java.io.IOException

class DownloadTask(
    private val music: Music,
    private val downloadDir: File,
    private val onProgress: (Int) -> Unit,
    private val onError: (String) -> Unit,
    private val onSuccess: () -> Unit,
    private val musicDao: MusicDao
) {
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
        private const val TAG = "MusicListAdapter"
    }

    private val apiService = RetrofitClient.getRetrofit().create(ApiService::class.java)
    private var currentCall: Call? = null

    private var localDownloadSize: Long = 0L
    private var totalDownloadSize: Long = 0L

    private fun checkDownloadDir() {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
    }

    private fun getFile(): File {
        val fileName = "${music.id}_${music.title}.mp3"
        return File(downloadDir, fileName)
    }

    private fun getRangeHeader(): String {
        val rangeHeader = if (localDownloadSize > 0) {
            "bytes=${localDownloadSize}-"
        } else {
            ""
        }
//        Log.d(TAG, "getRangeHeader: $rangeHeader")
        return rangeHeader
    }

    fun start() {
        if (music.downloaded) return

        music.downloading = true
        CoroutineScope(Dispatchers.IO).launch {
            if (musicDao.getMusicById(music.id) == null) {
                musicDao.insert(music)
            } else {
                musicDao.update(music)
            }
//            Log.d(TAG, "start: $music")
            checkDownloadDir()
            var file = getFile()

            if (localDownloadSize > totalDownloadSize) {
                file.delete()
                file = getFile()
            }
            localDownloadSize = file.length()
            val response = apiService.downloadFileWithDynamicUrlAsync(music.url, getRangeHeader())

            if (response.isSuccessful) {
                val contentRange = response.headers()["Content-Range"]
                if (contentRange != null) {
                    val size = contentRange.split("/").lastOrNull()?.toLongOrNull()
                    if (size != null) {
                        totalDownloadSize = size
                    } else {
                        handleErrorResponse(response)
                        return@launch
                    }
                }
                handleSuccessfulResponse(response.body(), file)
            } else {
                handleErrorResponse(response)
            }

            music.downloading = false
        }
    }

    suspend fun cancel() {
//        Log.d(TAG, "cancel: $music")
        withContext(Dispatchers.Main) {
            music.downloading = false
            music.downloaded = false

            musicDao.update(music)
        }
        currentCall?.cancel()
    }

    private fun deleteFile() {
        val file = getFile()
        if (file.exists()) {
            file.delete()
        }
    }

    private suspend fun handleSuccessfulResponse(responseBody: ResponseBody?, file: File) {
        responseBody?.let { body ->
            if (body.contentLength() > 0) {
                if (localDownloadSize == 0L) {
                    totalDownloadSize = body.contentLength()
                }

                if (totalDownloadSize == 0L) {
                    onError("下载失败，文件为空")
                    return
                }

//                Log.d(TAG, "body数据: ${body.contentLength()}")
//                Log.d(TAG, "handleSuccessfulResponse: ${music.id}|${file.length()}|${totalDownloadSize}")

                saveFile(body, file)

            } else {
                onError("下载失败，文件是空的")
            }
        }
    }

    private suspend fun handleErrorResponse(response: Response<ResponseBody>) {
        cancel()
        val error = response.errorBody()?.string() ?: "未知错误"
        onError("下载失败: $error")
    }

    private suspend fun checkFileDownloadCompletion(file: File) {
        withContext(Dispatchers.Main) {
            if (file.length() == totalDownloadSize) {
                music.downloaded = true
                musicDao.update(music)
                onSuccess()
            } else {
                onError("取消下载")
            }

        }
    }

    private suspend fun saveFile(responseBody: ResponseBody, file: File) {
//        Log.d(TAG, "数据库文件情况: localDownloadSize${localDownloadSize}|downloadTotalSize${totalDownloadSize}")
        val totalBytesDownloaded = copyResponseToFile(responseBody, file)

        withContext(Dispatchers.Main) {
            val progress = (totalBytesDownloaded * 100 / responseBody.contentLength()).toInt()
            onProgress(progress)
        }
    }

    private suspend fun copyResponseToFile(responseBody: ResponseBody, file: File): Long {
        var totalBytesDownloaded = localDownloadSize
        var bytesRead: Long = 0

        val isAppending = localDownloadSize > 0
        val sink = if (isAppending) file.sink(true).buffer() else file.sink().buffer()

        val source = responseBody.source()

        try {
            while (music.downloading) {
                bytesRead = source.read(sink.buffer, DEFAULT_BUFFER_SIZE.toLong())
                if (bytesRead != -1L) {
                    sink.emit()
                    totalBytesDownloaded += bytesRead

                    if (totalBytesDownloaded % (1024 * 2) == 0L) {
                        localDownloadSize = totalBytesDownloaded
                    }

                    withContext(Dispatchers.Main) {
                        val progress = (totalBytesDownloaded * 100 / totalDownloadSize).toInt()
                        onProgress(progress)
                    }


                } else {
                    break
                }
            }
        } catch (e: IOException) {
            onError("保存文件时发生错误: ${e.message}")
        } finally {
            withContext(Dispatchers.IO) {
                sink.close()
                source.close()
                checkFileDownloadCompletion(file)
            }
        }
        return totalBytesDownloaded
    }
}