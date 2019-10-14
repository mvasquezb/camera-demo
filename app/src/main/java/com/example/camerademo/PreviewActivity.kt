package com.example.camerademo

import android.media.*
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

class PreviewActivity : AppCompatActivity() {

    private lateinit var saveDir: File
    private lateinit var videoPath: String
    private var songPlayer: MediaPlayer? = null
    val song = Song.defaultSong

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        videoPath = intent.getStringExtra("video")!!
        saveDir = File(intent.getStringExtra("saveDir")!!)

        fab.setOnClickListener { view ->
            lifecycleScope.launchWhenCreated {
                testVideoMuxDemux()
            }
        }

        videoPlayer.setVideoPath(videoPath)
        videoPlayer.setOnCompletionListener {
            songPlayer?.runCatching {
                stop()
                reset()
                start()
            }
            videoPlayer.start()
        }
    }

    override fun onResume() {
        startSongPlayer()
        videoPlayer.start()
        super.onResume()
    }

    private fun startSongPlayer() {
        setupSongPlayer()
        songPlayer?.start()
    }

    override fun onPause() {
        stopSongPlayer()
        videoPlayer.stopPlayback()
        super.onPause()

    }

    private fun stopSongPlayer() {
        songPlayer?.release()
        songPlayer = null
    }

    private fun setupSongPlayer() {
        songPlayer = MediaPlayer().apply {
            setDataSource(song.mp3)
//            isLooping = true
            prepare()
            setVolume(0.2f, 0.2f)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun testVideoMuxDemux() {
        withContext(Dispatchers.IO) {
            val video = File(videoPath)
            val tracks = demuxVideo(video)

            val videoTrack = tracks["video"] ?: error("Video track missing")
            val audioTrack = tracks["audio"] ?: error("Audio track missing")

            videoTrack.apply {
                val outFile = File(saveDir, "demuxedVideo.mp4")
                this.copyTo(outFile, true)
            }

            audioTrack.apply {
                val outFile = File(saveDir, "demuxedAudio.m4a")
                this.copyTo(outFile, true)
            }

            val newVideo = muxVideo(videoTrack, audioTrack)
            newVideo.apply {
                val outFile = File(saveDir, "remuxedVideo.mp4")
                this.copyTo(outFile, true)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PreviewActivity, "Video files ready", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun muxVideo(videoTrack: File, audioTrack: File): File {
        val tmpOut = FFmpegManager.muxVideoAudio(videoTrack, audioTrack, saveDir)
        return tmpOut
    }

    private fun demuxVideo(video: File): Map<String, File> {
        val tracks = FFmpegManager.demuxVideoAudio(video, saveDir)
        return tracks
    }
}
