package com.example.camerademo

import android.app.Application
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.camerademo.lrcplayer.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val song: Song = Song.defaultSong
    var savedPath = ""
    private var numFiles = 0

    val saveDir = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!

    private val _loading = MutableLiveData<Boolean>().apply { value = false }
    val loading: LiveData<Boolean> = _loading

    private val _previewReady = MutableLiveData<Boolean>().apply { value = false }
    val previewReady: LiveData<Boolean> = _previewReady

    private var previousState = MutableLiveData<RecordingState>().apply { value = RecordingState.IDLE }
    private val _recordingState = MutableLiveData<RecordingState>().apply { value = RecordingState.IDLE }
    val recordingState: LiveData<RecordingState> = _recordingState

    val mainHandler = Handler(Looper.getMainLooper())

    // 1 for lyrics/song, 1 for video/audio recording
    private var barrier: CountDownLatch? = CountDownLatch(2)

    private val _isRecordingVideo = MutableLiveData<Boolean>().apply { value = false }
    private var isRecordingVideo: LiveData<Boolean> = _isRecordingVideo

    suspend fun handleVideo(video: File) {
        _loading.value = true
        val savePath = getVideoFilePath()
        numFiles++
        val outFile = File(savePath)
        withContext(Dispatchers.IO) {
            video.copyTo(outFile, true)
            withContext(Dispatchers.Main) {
                _loading.value = false
            }
        }
    }

    suspend fun processVideos() {
        _loading.value = true
        withContext(Dispatchers.IO) {
            val files = (0 until numFiles).map {
                File(saveDir, "video_$it.mp4").absolutePath
            }
            val output = FFmpegManager.concat(files, saveDir)
            val saveFile = File(saveDir, "video.mp4")
            output.copyTo(saveFile, true)
            savedPath = saveFile.absolutePath
            withContext(Dispatchers.Main) {
                _loading.value = false
                _previewReady.value = true
            }
        }
    }

    fun getVideoFilePath(): String {
        return "${saveDir.absolutePath}/video_$numFiles.mp4"
    }

    fun notifyPlayerReady() {
        barrier?.countDown()
    }

    fun notifyCameraReady(block: () -> Unit) {
        barrier?.countDown()
    }

    fun toggleVideoRecording() {
        _isRecordingVideo.value = !(_isRecordingVideo.value ?: false)
        if (barrier?.let { it.count == 2L } == false) {
            barrier = CountDownLatch(2)
        }
    }
}