package com.example.musicplayer.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.common.AppDatabase
import com.example.musicplayer.dao.MusicDao
import com.example.musicplayer.databinding.ActivityMusicListBinding
import com.example.musicplayer.ui.base.BaseActivity

class MusicListActivity : BaseActivity() {
    private lateinit var binding: ActivityMusicListBinding
    private lateinit var musicDao: MusicDao

    private val adapter: MusicListAdapter by lazy {
        MusicListAdapter()
    }

    private val viewModel: MusicViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 使用数据绑定
        binding = DataBindingUtil.setContentView(this, R.layout.activity_music_list)
        binding.lifecycleOwner = this

        // 配置适配器
        binding.musicRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.musicRecyclerView.adapter = adapter

        musicDao = AppDatabase.getInstance(this).musicDao()
        viewModel.loadTracks(musicDao)

        // 设置下拉刷新逻辑
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTracks(musicDao)
            binding.swipeRefreshLayout.isEnabled = false
        }

        // 监听音乐列表
        viewModel.tracks.observe(this) { tracks ->
            adapter.submitList(tracks)
            binding.totalTextView.visibility = if (tracks.isNotEmpty()) View.VISIBLE else View.GONE
            binding.refreshTextView.visibility = if (tracks.isEmpty()) View.VISIBLE else View.GONE
            binding.totalTextView.text = "共${tracks.size}首"
        }

        // 监听loading
        viewModel.loading.observe(this) { loading ->
            // 如果在加载中，禁用下拉刷新
            binding.swipeRefreshLayout.isEnabled = !loading

            binding.swipeRefreshLayout.isRefreshing = loading
            if (loading) {
                binding.refreshTextView.text = "加载中..."
            } else {
                binding.refreshTextView.text = "下拉刷新"
                Toast.makeText(this, "刷新完成", Toast.LENGTH_SHORT).show()
            }
        }
    }
}