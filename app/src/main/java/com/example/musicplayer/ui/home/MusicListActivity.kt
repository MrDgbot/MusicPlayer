package com.example.musicplayer

import MusicViewModel
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.databinding.ActivityMusicListBinding
import com.example.musicplayer.ui.home.MusicListAdapter


class MusicListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMusicListBinding
    private lateinit var adapter: MusicListAdapter
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_music_list)
        adaptImmersiveStatusBar()

        // 观察tracks属性的更改
        viewModel.tracks.observe(this) { tracks ->
            adapter.setData(tracks)
        }

        // 观察loading属性的更改
        viewModel.loading.observe(this) { loading ->
            binding.swipeRefreshLayout.isRefreshing = loading
            if (!loading) {
                Toast.makeText(this, "刷新完成", Toast.LENGTH_SHORT).show()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTracks()
        }

        binding.musicRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MusicListAdapter()
        binding.musicRecyclerView.adapter = adapter

        viewModel.loadTracks()
    }

    private fun adaptImmersiveStatusBar() {
        // 在这里您可以实现沉浸式状态栏的逻辑
    }
}