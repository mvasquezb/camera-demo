package com.example.camerademo

import android.app.Application
import net.gotev.uploadservice.Logger
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.okhttp.OkHttpStack

class CameraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Logger.setLogLevel(Logger.LogLevel.DEBUG)
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID
        UploadService.HTTP_STACK = OkHttpStack()
    }
}