package com.example.camerademo.camera2

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.RecommendedStreamConfigurationMap
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.example.camerademo.camera2.events.Error
import com.example.camerademo.camera2.events.EventDispatcher
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class CameraHelper(val context: Context, val eventDispatcher: EventDispatcher) {

    companion object {
        @JvmStatic val TAG = CameraHelper::class.java.simpleName
        @JvmStatic val MEDIA_TYPE_IMAGE = 1
        @JvmStatic val MEDIA_TYPE_VIDEO = 2
    }

    private var videoCallback: VideoCallback? = null
    private lateinit var outputVideoFile: File
    lateinit var texture: TextureView
    private var cameraId: String = ""
    private var mediaRecorder: MediaRecorder? = null
    var facing: Int = Defaults.DEFAULT_FACING

    private var manager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * A reference to the opened [android.hardware.camera2.CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * A reference to the current [android.hardware.camera2.CameraCaptureSession] for
     * preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private lateinit var previewSize: Size

    /**
     * The [android.util.Size] of video recording.
     */
    private lateinit var videoSize: Size

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraHelper.cameraDevice = cameraDevice
            startPreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraHelper.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraHelper.cameraDevice = null
        }
    }

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    /**
     * Iterate over supported camera video sizes to see which one best fits the
     * dimensions of the given view while maintaining the aspect ratio. If none can,
     * be lenient with the aspect ratio.
     *
     * @param supportedVideoSizes Supported camera video sizes.
     * @param previewSizes Supported camera preview sizes.
     * @param w     The width of the view.
     * @param h     The height of the view.
     * @return Best match camera video size to fit in the view.
     */
    fun getOptimalVideoSize(
        supportedVideoSizes: List<Size>?,
        previewSizes: List<Size>, w: Int, h: Int
    ): Size? {
        // Use a very small tolerance because we want an exact match.
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = w.toDouble() / h

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        val videoSizes = supportedVideoSizes ?: previewSizes
        var optimalSize: Size? = null

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        var minDiff = java.lang.Double.MAX_VALUE

        // Target view height

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (size in videoSizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue
            if (Math.abs(size.height - h) < minDiff && previewSizes.contains(size)) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in videoSizes) {
                if (Math.abs(size.height - h) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    /**
     * @return the default rear/back facing camera on the device. Returns null if camera is not
     * available.
     */
    fun getDefaultBackCamera(): String {
        return manager.cameraIdList[0]
    }

    /**
     * @return the default front facing camera on the device. Returns null if camera is not
     * available.
     */
    fun getDefaultFrontCamera(): String {
        return manager.cameraIdList[1]
    }

    /**
     *
     * @param position Physical position of the camera i.e Camera.CameraInfo.CAMERA_FACING_FRONT
     * or Camera.CameraInfo.CAMERA_FACING_BACK.
     * @return the default camera on the device. Returns null if camera is not available.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    fun getDefaultCamera(): String? {
        // Find front-facing camera, or back facing if not found
        var backCamId = ""
        manager.cameraIdList.forEach { camId ->
            val characteristics = manager.getCameraCharacteristics(camId)
            if (characteristics[CameraCharacteristics.LENS_FACING]
                == CameraCharacteristics.LENS_FACING_FRONT) {
                return camId
            } else if (characteristics[CameraCharacteristics.LENS_FACING]
                == CameraCharacteristics.LENS_FACING_BACK && backCamId.isEmpty()) {
                backCamId = camId
            }
        }

        return if (backCamId.isNotEmpty()) {
            backCamId
        } else {
            null
        }
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
//    fun chooseVideoSize(choices: Array<Size>) = choices.firstOrNull {
//        it.width == it.height * 4 / 3 && it.width <= 1080} ?: choices[choices.size - 1]
    fun chooseVideoSize(choices: Array<Size>, size: Size): Size {
        // Use a very small tolerance because we want an exact match.
        val ASPECT_TOLERANCE = 0.1
        val width = size.width
        val height = size.height
        val targetRatio = width.toDouble() / height

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        val videoSizes = choices.toList()
        var optimalSize: Size? = null

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        var minDiff = java.lang.Double.MAX_VALUE

        // Target view height

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (size in videoSizes) {
            val ratio = width.toDouble() / height
            if (abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue
            if (abs(size.height - height) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - height).toDouble()
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in videoSizes) {
                if (abs(size.height - height) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - height).toDouble()
                }
            }
        }
        return optimalSize!!
    }

    /**
     * Given [choices] of [Size]s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal [Size], or an arbitrary one if none were big enough
     */
    fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.height == it.width * h / w && it.width >= width && it.height >= height }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.isNotEmpty()) {
            bigEnough.minBy { it.width * it.height }!!
        } else {
            choices[0]
        }
    }
    
    /**
     * Creates a media file in the `Environment.DIRECTORY_PICTURES` directory. The directory
     * is persistent and available to other applications like gallery.
     *
     * @param type Media type. Can be video or image.
     * @return A file object pointing to the newly created file.
     */
    fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED,
                ignoreCase = true
            )
        ) {
            return null
        }

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "CameraSample"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory")
                return null
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val mediaFile: File
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = File(
                mediaStorageDir.path + File.separator +
                        "IMG_" + timeStamp + ".jpg"
            )
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = File(
                (mediaStorageDir.path + File.separator +
                        "VID_" + timeStamp + ".mp4")
            )
        } else {
            return null
        }
        Log.d("camerahelper", "out file: " + mediaFile.absolutePath)
        return mediaFile
    }

    fun getPreviewSize(size: Size): Size? {
        cameraId = getActualCameraId() ?: return null

        // Choose the sizes for camera preview and video recording
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Could not get camera config map")

        videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java), size)
        Log.d(TAG, "videoSize: $videoSize")
        previewSize = chooseOptimalSize(
            map.getOutputSizes(SurfaceTexture::class.java), size.height, size.width, videoSize)
        Log.d(TAG, "previewSize: $previewSize")

        return previewSize
    }

    private fun getActualCameraId(): String? {
        return when (facing) {
            Constants.DIRECTION_FRONT -> getDefaultFrontCamera()
            Constants.DIRECTION_BACK -> getDefaultBackCamera()
            else -> getDefaultCamera()
        }
    }

    fun start() {
        startBackgroundThread()
        openCamera()
    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by [stateCallback].
     *
     * Lint suppression - permission is checked in [hasPermissionsGranted]
     */
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            cameraId = getActualCameraId() ?: throw RuntimeException("No camera available")
            manager.openCamera(cameraId, stateCallback, null)
            getPreviewSize(previewSize)
        } catch (e: CameraAccessException) {
            eventDispatcher.dispatch(Error(e))
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            eventDispatcher.dispatch(Error(e))
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    fun stop() {
        if (videoCallback != null) {
            stopRecording()
        }
        closeCamera()
        stopBackgroundThread()
    }

    /**
     * Close the [CameraDevice].
     */
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closePreviewSession()
            cameraDevice?.close()
            cameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (cameraDevice == null) return

        try {
            closePreviewSession()
            val texture = texture.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            val previewSurface = Surface(texture)
            previewRequestBuilder.addTarget(previewSurface)

            cameraDevice?.createCaptureSession(listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        eventDispatcher.dispatch(Error())
                    }
                }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
            eventDispatcher.dispatch(Error(e))
        }
    }

    /**
     * Update the camera preview. [startPreview] needs to be called in advance.
     */
    private fun updatePreview() {
        if (cameraDevice == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(),
                null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
            eventDispatcher.dispatch(Error(e))
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputVideoFile.absolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(videoSize.width, videoSize.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
        }
    }

    fun startVideo(videoCallback: VideoCallback) {
        if (cameraDevice == null) return

        try {
            outputVideoFile = createTempFile(suffix = ".mp4")
            this.videoCallback = videoCallback

            closePreviewSession()
            setUpMediaRecorder()
            val texture = texture.surfaceTexture.apply {
                setDefaultBufferSize(previewSize.width, previewSize.height)
            }

            // Set up Surface for camera preview and MediaRecorder
            val previewSurface = Surface(texture)
            val recorderSurface = mediaRecorder!!.surface
            val surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                add(recorderSurface)
            }
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice?.createCaptureSession(surfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        captureSession = cameraCaptureSession
                        updatePreview()
                        mediaRecorder?.start()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        eventDispatcher.dispatch(Error())
                    }
                }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())

        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            reset()
        }
    }

    fun stopVideo() {
        stopRecording()
        videoCallback?.invoke(outputVideoFile)
        videoCallback = null
        startPreview()
    }
}