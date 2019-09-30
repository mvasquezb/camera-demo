package com.example.camerademo

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    var filePath = ""
        private set

    private var numFiles = 0

    val context = getApplication<Application>().applicationContext

    val saveDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        _loading.value = false
    }

    suspend fun handleVideo(video: File) {
        var savePath = "video"
        if (numFiles != 0) {
            savePath += "_$numFiles"
        }
        savePath += ".mp4"
        if (filePath.isEmpty()) {
            filePath = savePath
        }
        numFiles++
        val outFile = File(saveDir, savePath)
        _loading.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                video.copyTo(outFile, true)
                withContext(Dispatchers.Main) {
                    _loading.value = false
                }
            }
        }
    }
}