package com.example.camerademo

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private var filePath = ""
    private var numFiles = 0

    val context = getApplication<Application>().applicationContext

    val saveDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        _loading.value = false
    }

    suspend fun handleVideo(video: File) {
        _loading.value = true
        var savePath = "video_$numFiles"
        savePath += ".mp4"
        if (filePath.isEmpty()) {
            filePath = savePath
        }
        numFiles++
        val outFile = File(saveDir, savePath)
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
            withContext(Dispatchers.Main) {
                _loading.value = false
            }
        }
    }
}