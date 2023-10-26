package com.example.musicplayer.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.dao.MusicDao
import com.example.musicplayer.data.Music
import com.example.musicplayer.network.ApiService
import com.example.musicplayer.network.RetrofitClient
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    private val apiService: ApiService = RetrofitClient.getRetrofit().create(ApiService::class.java)

    private val _tracks = MutableLiveData<List<Music>>()
    val tracks: LiveData<List<Music>> get() = _tracks

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun loadTracks(musicDao: MusicDao) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val responseTracks = apiService.getTracks().tracks

                // 获取所有音乐的 id
                val trackIds = responseTracks.map { it.id }

                // 从数据库获取音乐
                val dbTracks = musicDao.getMusicsByIds(trackIds)
                Log.d("MusicViewModel", "dbTracks: $dbTracks")
                // 创建映射便于快速查找
                val dbTrackMap = dbTracks.associateBy { it.id }

                val finalTracks = responseTracks.map { dbTrackMap[it.id] ?: it }
                Log.d("MusicViewModel", "loadTracks: $finalTracks")
                _tracks.value = finalTracks
            } catch (e: Exception) {
                _tracks.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}