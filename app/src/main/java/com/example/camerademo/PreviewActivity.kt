package com.example.camerademo

import android.media.*
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        setSupportActionBar(toolbar)

        videoPath = intent.getStringExtra("video")!!
        saveDir = File(intent.getStringExtra("saveDir")!!)

        fab.setOnClickListener { view ->
            lifecycleScope.launchWhenCreated {
                testVideoMuxDemux()
            }
        }

        videoPlayer.setVideoPath(videoPath)
        videoPlayer.start()
    }

    private suspend fun testVideoMuxDemux() {
        withContext(Dispatchers.IO) {
            val video = File(videoPath)
            val tracks = demuxVideo(video)

            val videoTrack = tracks["video"]!!
            val audioTrack = tracks["audio"]!!

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

    private fun extractMediaTrack(extractor: MediaExtractor, trackIndex: Int): File {
        val outFile = createTempFile(directory = saveDir)
        val outStream = outFile.outputStream()
        val channel = outStream.channel

        extractor.selectTrack(trackIndex)
        val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
        while (extractor.readSampleData(buffer, 0) >= 0) {
            channel.write(buffer)
            if (!buffer.hasRemaining()) {
                buffer.clear()
            }
            extractor.advance()
        }
        channel.close()
        return outFile
    }
}
