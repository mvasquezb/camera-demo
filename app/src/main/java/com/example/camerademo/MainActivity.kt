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
import com.example.camerademo.lrcplayer.LrcPlayer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import java.util.concurrent.CyclicBarrier

class MainActivity : AppCompatActivity() {

    companion object {
        @JvmStatic val TAG = MainActivity::class.java.simpleName
    }

    private var isRecording = false
    private lateinit var viewModel: MainViewModel
    private lateinit var lrcPlayer: LrcPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupVideoRecording()
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        setupRecording()
        setupVMObservers()
    }

    private fun setupVMObservers() {
        viewModel.loading.observe(this, Observer { loading ->
            when (loading) {
                true -> loadingBar.visibility = View.VISIBLE
                else -> loadingBar.visibility = View.GONE
            }
        })
        viewModel.previewReady.observe(this, Observer {
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
        viewModel.recordingState.observe(this, Observer { state ->
            when (state) {
                RecordingState.STARTED -> {

                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        camera.start()
    }

    override fun onPause() {
        camera.stop()
        super.onPause()
    }

    private fun showToast(message : String) = Toast.makeText(this, message, LENGTH_SHORT).show()

    private fun setupRecording() {
        playButton.setOnClickListener {
            toggleRecording()
        }
        finishButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            lifecycleScope.launch {
                viewModel.processVideos()
            }
        }

        setupVideoRecording()
        setupLrcPlayer()
    }

    private fun setupVideoRecording() {
        camera.addEventListener(object : CameraEventListener {
            override fun onError(error: Error) {
                Log.e(TAG, "error: ${error.error?.message}")
                showToast(error.error?.message ?: "Error connecting to camera")
            }
        })
    }

    private fun setupLrcPlayer() {
        lrcPlayer = LrcPlayer(viewModel.song)
        lrcPlayer.prepare { mp ->
            viewModel.notifyPlayerReady()
        }
    }

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        isRecording = true
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause))
        viewModel.notifyCameraReady {
            camera.startVideo { video ->
                lifecycleScope.launch {
                    viewModel.handleVideo(video)
                    showToast("Video saved: ${viewModel.getVideoFilePath()}")
                }
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play))
        camera.stopVideo()
    }

    private fun toggleVideo() {
        viewModel.toggleVideoRecording()
    }
}
