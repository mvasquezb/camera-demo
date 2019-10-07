package com.example.camerademo

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.DownloadListener
import com.example.camerademo.lrc.LrcFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _lrcFile = MutableLiveData<LrcFile>()
    val lrcFile: LiveData<LrcFile> = _lrcFile
    val song = Song.defaultSong

    var savedPath = ""
    private var numFiles = 0

    val context = getApplication<Application>().applicationContext

    val saveDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!

    private val _loading = MutableLiveData<Boolean>().apply { value = false }
    val loading: LiveData<Boolean> = _loading

    private val _previewReady = MutableLiveData<Boolean>().apply { value = false }
    val previewReady: LiveData<Boolean> = _previewReady

    var currentPosition: Int = 0

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
        Log.d("viewmodel", "files: $numFiles")
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

    fun downloadLrcFile() {
        downloadLrcFile(song.lyric)
    }

    fun downloadLrcFile(url: String) {
        _loading.value = true
        viewModelScope.launch {
            AndroidNetworking.download(url, saveDir.absolutePath, "lyrics.lrc")
                .setPriority(Priority.IMMEDIATE)
                .build()
                .startDownload(object : DownloadListener {
                    override fun onDownloadComplete() {
                        viewModelScope.launch {
                            withContext(Dispatchers.IO) {
                                val lrcFile = buildLrcFile()
                                withContext(Dispatchers.Main) {
                                    _lrcFile.value = lrcFile
                                    _loading.value = false
                                }
                            }
                        }
                    }

                    override fun onError(anError: ANError?) {
                        Log.e("ViewModel", "error: $anError")
                    }
                })
        }
    }

    private fun buildLrcFile(): LrcFile {
        return LrcFile(File(saveDir, "lyrics.lrc"))
    }
}