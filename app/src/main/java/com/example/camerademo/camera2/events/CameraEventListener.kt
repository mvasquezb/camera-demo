package com.example.camerademo.camera2.events

interface CameraEventListener {
    fun onError(error: Error)
    fun onEvent(event: Event) = Unit
}