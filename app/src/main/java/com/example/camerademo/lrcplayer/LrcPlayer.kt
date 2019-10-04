package com.example.camerademo.lrcplayer

import android.media.MediaPlayer

class LrcPlayer(var song: Song) {
    private var mediaPlayer: MediaPlayer? = null
    val isAvailable: Boolean
        get() = mediaPlayer != null

    fun prepare(listener: (mp: MediaPlayer) -> Unit) {
        clear()
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener(listener)
            setDataSource(song.mp3)
            prepareAsync()
        }
    }

    private fun clear() {
        // Ignore result
        mediaPlayer?.runCatching {
            stop()
            reset()
            release()
        }
        mediaPlayer = null
    }

    fun start() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.stop()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun seekTo(msec: Int) {
        mediaPlayer?.seekTo(msec)
    }
}