package com.example.camerademo

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.example.camerademo.camera2.events.CameraEventListener
import com.example.camerademo.camera2.events.Error
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        @JvmStatic val TAG = MainActivity::class.java.simpleName
    }

    private var isRecording = false
    private lateinit var viewModel: MainViewModel
    private var songPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        setupVideoRecording()
        setupSongPlayer()

        viewModel.loading.observe(this, Observer<Boolean> { loading ->
            when (loading) {
                true -> loadingBar.visibility = View.VISIBLE
                else -> loadingBar.visibility = View.GONE
            }
        })
        viewModel.previewReady.observe(this, Observer<Boolean> {
            when (it) {
                true -> {
                    val intent = Intent(this, PreviewActivity::class.java).apply {
                        putExtra("video", viewModel.savedPath)
                        putExtra("saveDir", viewModel.saveDir.absolutePath)
                    }
                    startActivity(intent)
                }
            }
        })
    }

    private fun setupSongPlayer() {
        songPlayer = MediaPlayer().apply {
            setDataSource(viewModel.song.mp3)
            prepare()
            if (viewModel.currentPosition != 0) {
                seekTo(viewModel.currentPosition)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        camera.start()
        setupSongPlayer()
    }

    override fun onPause() {
        onRecordingStop()
        super.onPause()
    }

    private fun stopSongPlayer(reset: Boolean = true) {
        songPlayer?.runCatching {
            viewModel.currentPosition = currentPosition
            release()
        }
        songPlayer = null
    }

    private fun pauseSongPlayer() {
        songPlayer?.runCatching {
            viewModel.currentPosition = currentPosition
            pause()
        }
    }

    private fun startSongPlayer() {
        songPlayer?.runCatching {
            start()
        }
    }

    private fun showToast(message : String) = Toast.makeText(this, message, LENGTH_SHORT).show()

    private fun setupVideoRecording() {
        playButton.setOnClickListener {
            toggleRecording()
        }
        finishButton.setOnClickListener {
            onRecordingFinish()
        }
        camera.addEventListener(object : CameraEventListener {
            override fun onError(error: Error) {
                Log.e(TAG, "error: ${error.error?.message}")
                showToast(error.error?.message ?: "Error connecting to camera")
            }
        })
    }

    private fun toggleRecording() {
        if (!isRecording) {
            onRecordingStart()
        } else {
            onRecordingPause()
        }
    }

    private fun uiStartRecording() {
        isRecording = true
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause))
    }

    private fun uiPauseRecording() {
        isRecording = false
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play))
    }

    private fun onRecordingStart() {
        uiStartRecording()
        startSongPlayer()
        camera.startVideo { video ->
            Log.d(TAG, "video callback: ${video.absolutePath}")
            lifecycleScope.launch {
                showToast("Video saved: ${viewModel.getVideoFilePath()}")
                viewModel.handleVideo(video)
            }
        }
    }

    private fun onRecordingPause() {
        uiPauseRecording()
        camera.stopVideo()
        pauseSongPlayer()
    }

    private fun onRecordingStop() {
        uiPauseRecording()
        camera.stop()
        stopSongPlayer()
    }

    private fun onRecordingFinish() {
        if (isRecording) {
            onRecordingStop()
        }
        lifecycleScope.launch {
            viewModel.processVideos()
        }
    }
}
