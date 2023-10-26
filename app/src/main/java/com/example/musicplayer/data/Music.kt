package com.example.musicplayer.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Music(
    @PrimaryKey val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val url: String,
    val cover: String,
    var downloading: Boolean = false,
    var downloaded: Boolean = false,
    // 播放状态
    var playing: Boolean = false,
    // 当前播放位置
    var currentPosition: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString().orEmpty(),
        parcel.readString().orEmpty(),
        parcel.readInt(),
        parcel.readString().orEmpty(),
        parcel.readString().orEmpty()
    ) {
        downloading = parcel.readByte() != 0.toByte()
        downloaded = parcel.readByte() != 0.toByte()
        playing = parcel.readByte() != 0.toByte()
        currentPosition = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeInt(year)
        parcel.writeString(url)
        parcel.writeString(cover)
        parcel.writeByte(if (downloading) 1 else 0)
        parcel.writeByte(if (downloaded) 1 else 0)
        parcel.writeByte(if (playing) 1 else 0)
        parcel.writeInt(currentPosition)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Music> {
        override fun createFromParcel(parcel: Parcel): Music {
            return Music(parcel)
        }

        override fun newArray(size: Int): Array<Music?> {
            return arrayOfNulls(size)
        }
    }
}