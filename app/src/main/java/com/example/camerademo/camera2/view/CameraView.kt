package com.example.camerademo.camera2.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Size
import com.example.camerademo.camera2.CameraHelper
import com.example.camerademo.R
import com.example.camerademo.camera2.Defaults
import com.example.camerademo.camera2.events.EventDispatcher
import com.example.camerademo.camera2.events.CameraEventListener
import com.example.camerademo.camera2.VideoCallback

open class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AutofitTextureView(context, attrs, defStyle) {
    private val eventDispatcher = EventDispatcher()
    private lateinit var size: Size

    private var adjustViewBounds: Boolean = attrs?.getAttributeBooleanValue(
        R.styleable.KS_android_adjustViewBounds, Defaults.DEFAULT_ADJUST_BOUNDS
    ) ?: Defaults.DEFAULT_ADJUST_BOUNDS

    private var facing: Int = 0
        set(value) {
            cameraHelper.facing = value
            field = value
        }

    private val cameraHelper = CameraHelper(context, eventDispatcher)

    private val textureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture?, width: Int, height: Int) = Unit

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture?): Boolean = true

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture?, width: Int, height: Int) {
            size = Size(width, height)
            cameraHelper.texture = this@CameraView
            start()
        }
    }

    init {
        surfaceTextureListener = textureListener
        facing = attrs?.getAttributeIntValue(
            R.styleable.KS_facing, Defaults.DEFAULT_FACING
        ) ?: Defaults.DEFAULT_FACING
    }

    fun start() {
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (isAvailable) {
            updatePreviewSize()
            cameraHelper.start()
        } else {
            surfaceTextureListener = textureListener
        }
    }

    private fun updatePreviewSize() {
        val previewSize = cameraHelper.getPreviewSize(size)
        previewSize?.let { setAspectRatio(it.width, it.height) }
    }

    fun stop() {
        cameraHelper.stop()
    }

    fun addEventListener(listener: CameraEventListener) {
        eventDispatcher.addListener(listener)
    }

    fun startVideo(videoCallback: VideoCallback) {
        if (isAvailable) {
            cameraHelper.startVideo(videoCallback)
        }
    }

    fun stopVideo() {
        cameraHelper.stopVideo()
    }
}