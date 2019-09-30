package com.example.camerademo

import android.app.Application
import android.os.AsyncTask
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    fun handleVideo(video: File) {
        _loading.value = true
        var savePath = "video_$numFiles"
        savePath += ".mp4"
        if (filePath.isEmpty()) {
            filePath = savePath
        }
        numFiles++
        val outFile = File(saveDir, savePath)
        val saveTask = SaveVideoTask {
            _loading.value = false
        }
        saveTask.execute(video, outFile)
    }

    fun processVideos() {
        _loading.value = true
        val saveTask = MergeVideoTask(numFiles, saveDir!!) {
            _loading.value = false
        }
        saveTask.execute()
    }

    class MergeVideoTask(
        val numFiles: Int,
        val saveDir: File,
        val callback: () -> Unit
    ) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            val savePath = "video"

            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            callback()
        }
    }

    class SaveVideoTask(val callback: () -> Unit) : AsyncTask<File, Void, Void>() {
        override fun doInBackground(vararg files: File?): Void? {
            val video = files[0]
            val outFile = files[1]
            video!!.copyTo(outFile!!, true)
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            callback()
        }
    }
}