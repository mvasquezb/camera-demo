package com.example.camerademo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.wonderkiln.camerakit.*
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
    }

    private fun setupVideoRecording() {
        camera.addCameraKitListener(object : CameraKitEventListener {
            override fun onVideo(video: CameraKitVideo?) {
                Log.d(TAG, "video: ${video?.message}")
            }

            override fun onEvent(event: CameraKitEvent?) {
                Log.d(TAG, "event: ${event?.message}")
            }

            override fun onImage(image: CameraKitImage?) {
                Log.d(TAG, "image: ${image?.message}")
            }

            override fun onError(error: CameraKitError?) {
                Log.d(TAG, "error: ${error?.message}")
            }
        })
        playButton.setOnClickListener {
            if (!isRecording) {
                playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause))
                camera.captureVideo { video ->
                    Log.d(TAG, "video captured")
                    lifecycleScope.launch {
                        viewModel.handleVideo(video.videoFile)
                    }
                }
            } else {
                playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play))
                camera.stopVideo()
            }
            isRecording = !isRecording
        }
    }

    override fun onStart() {
        super.onStart()
        camera.start()
    }

    override fun onStop() {
        super.onStop()
        camera.stop()
    }
}
