package com.example.musicplayer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.musicplayer.data.Music

@Dao
interface MusicDao {

    @Insert
    suspend fun insert(music: Music)

    @Insert
    suspend fun insertAll(musics: List<Music>)


    @Update
    suspend fun update(music: Music)

    @Query("SELECT * FROM Music WHERE id = :id")
    suspend fun getMusicById(id: Int): Music?

    @Query("SELECT * FROM Music WHERE id = :id AND url = :url")
    suspend fun getMusicByIdAndUrl(id: Int, url: String): Music?

    @Query("SELECT * FROM Music WHERE id IN (:ids)")
    suspend fun getMusicsByIds(ids: List<Int>): List<Music>

    @Query("SELECT * FROM Music")
    suspend fun getAllMusics(): List<Music>

    @Query("SELECT * FROM Music WHERE downloaded = 1")
    suspend fun getDownloadedMusics(): List<Music>
}