package com.example.musicplayer.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import com.example.musicplayer.data.Music


class MusicListAdapter : RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {

    private var data: List<Music> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Music>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = data[position]
        holder.bind(track)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(track: Music) {
            // 绑定数据到 itemView 中的视图，例如 TextView、ImageView
            itemView.findViewById<TextView>(R.id.title_text_view).text = track.title
            itemView.findViewById<TextView>(R.id.artist_text_view).text = track.artist
            itemView.findViewById<TextView>(R.id.year_text_view).text = track.year.toString()
            Glide.with(itemView)
                .load(track.cover)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(4)))
                .into(itemView.findViewById<ImageView>(R.id.music_cover))

        }
    }
}