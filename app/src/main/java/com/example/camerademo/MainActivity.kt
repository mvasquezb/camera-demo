package com.example.camerademo

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupVideoRecording()
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
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

    override fun onResume() {
        super.onResume()
        camera.start()
    }

    override fun onPause() {
        camera.stop()
        super.onPause()
    }

    private fun showToast(message : String) = Toast.makeText(this, message, LENGTH_SHORT).show()

    private fun setupVideoRecording() {
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
        camera.addEventListener(object : CameraEventListener {
            override fun onError(error: Error) {
                Log.e(TAG, "error: ${error.error?.message}")
                showToast(error.error?.message ?: "Error connecting to camera")
            }
        })
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
        camera.startVideo { video ->
            lifecycleScope.launch {
                viewModel.handleVideo(video)
                showToast("Video saved: ${viewModel.getVideoFilePath()}")
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play))
        camera.stopVideo()
    }
}
