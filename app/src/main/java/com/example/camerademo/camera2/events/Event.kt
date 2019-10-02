package com.example.camerademo.camera2.events

open class Event(val type: String) {
    companion object {
        @JvmStatic val ERROR_TYPE = "CameraError"
    }
}