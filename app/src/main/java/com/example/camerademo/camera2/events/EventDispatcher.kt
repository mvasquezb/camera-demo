package com.example.camerademo.camera2.events

import android.os.Handler
import android.os.Looper

class EventDispatcher(
    val listeners: MutableList<CameraEventListener> = mutableListOf()
) {
    val mainHandler = Handler(Looper.getMainLooper())
    fun addListener(listener: CameraEventListener) {
        listeners.add(listener)
    }

    fun dispatch(event: Event) {
        mainHandler.post {
            listeners.forEach {
                it.onEvent(event)
                when (event) {
                    is Error -> it.onError(event)
                }
            }
        }
    }
}