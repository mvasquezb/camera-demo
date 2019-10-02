package com.example.camerademo.camera2.events

class Error(val error: Throwable? = null) : Event(ERROR_TYPE)