package com.example.camerademo.upload

import android.content.Context
import android.util.Log
import net.gotev.uploadservice.UploadNotificationConfig
import net.gotev.uploadservice.MultipartUploadRequest
import java.io.File


object UploadManager {
    @JvmStatic val TAG = UploadManager::class.java.simpleName

    @JvmStatic val SERVER = "http://192.168.86.31:4000"

    fun uploadMultipart(context: Context, file: File, paramName: String) {
        try {
            val uploadId = MultipartUploadRequest(context, "$SERVER/upload")
                // starting from 3.1+, you can also use content:// URI string instead of absolute file
                .addFileToUpload(file.absolutePath, paramName)
                .setNotificationConfig(UploadNotificationConfig())
                .setAutoDeleteFilesAfterSuccessfulUpload(true)
                .setUsesFixedLengthStreamingMode(false)
                .setMaxRetries(2)
                .startUpload()
            Log.d(TAG, "upload: $uploadId")
        } catch (exc: Exception) {
            Log.e(TAG, exc.message, exc)
        }
    }

    fun uploadMultipleMultipart(context: Context, fileMap: Map<String, File>) {
        try {
            val uploadId = MultipartUploadRequest(context, "$SERVER/upload")
                .apply {
                    fileMap.entries.forEach {
                        // starting from 3.1+, you can also use content:// URI string instead of absolute file
                        addFileToUpload(it.value.absolutePath, it.key)
                    }
                }
                .setNotificationConfig(UploadNotificationConfig())
                .setMaxRetries(2)
                .startUpload()
            Log.d(TAG, "upload: $uploadId")
        } catch (exc: Exception) {
            Log.e(TAG, exc.message, exc)
        }
    }
}