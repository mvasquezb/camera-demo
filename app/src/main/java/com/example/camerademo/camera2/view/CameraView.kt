package com.example.camerademo.camera2.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import com.example.camerademo.R
import com.example.camerademo.activity
import com.example.camerademo.camera2.CameraHelper
import com.example.camerademo.camera2.Defaults
import com.example.camerademo.camera2.VideoCallback
import com.example.camerademo.camera2.events.CameraEventListener
import com.example.camerademo.camera2.events.EventDispatcher
import kotlin.math.max

open class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AutofitTextureView(context, attrs, defStyle) {
    private var previewSize: Size = Size(0, 0)
    private val eventDispatcher = EventDispatcher()
    private lateinit var size: Size

    private val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.CameraView)

    private var adjustViewBounds = styledAttrs.getBoolean(
        R.styleable.CameraView_android_adjustViewBounds, Defaults.DEFAULT_ADJUST_BOUNDS)

    var facing: Int = 0
        set(value) {
            cameraHelper.facing = value
            field = value
        }

    private val cameraHelper = CameraHelper(context, eventDispatcher)

    private val textureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture?, width: Int, height: Int) {
            configureTransform(width, height, previewSize)
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture?): Boolean = true

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture?, width: Int, height: Int) {
            size = Size(width, height)
            cameraHelper.texture = this@CameraView
            start()
        }
    }

    init {
        facing = styledAttrs.getInt(R.styleable.CameraView_camFacing, Defaults.DEFAULT_FACING)
        surfaceTextureListener = textureListener
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
        previewSize = cameraHelper.getPreviewSize(size) ?: return
        previewSize.let {
            setAspectRatio(it.height, it.width)
            configureTransform(this.width, this.height, previewSize)
        }
    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int, previewSize: Size) {
        val rotation = activity?.let { it.windowManager.defaultDisplay.rotation }
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = max(viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width)
            with(matrix) {
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        }
        setTransform(matrix)
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