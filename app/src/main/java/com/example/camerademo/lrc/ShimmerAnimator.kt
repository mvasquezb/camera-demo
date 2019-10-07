package com.example.camerademo.lrc

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.*
import android.text.TextPaint
import java.util.ArrayList
import java.util.HashMap

class ShimmerAnimator {

    private var animators: MutableList<ValueAnimator>? = ArrayList()
    private var animationFromX: MutableList<Int>? = ArrayList()
    private var animationLength: MutableList<Int>? = ArrayList()
    private val initialTime = ArrayList<Long>()
    private var currentAnimator = 0
    private var currentAnimationFromX: Float = 0.toFloat()
    private var currentAnimationLength: Float = 0.toFloat()
    private var shimmerBitmapWidth: Int = 0
    private var newDuration: Long = 0
    private var finished = false
    var isPaused = false
    var maskOffsetX: Float = 0.toFloat()
    var firstWait = true
    var gradientTexturePaint: Paint? = null
    var canvasForShimmerMask: Canvas? = null
    var maskBitmap: Bitmap? = null
    lateinit var maskRect: Rect


    private fun addAnimatorListeners(animator: ValueAnimator, callback: IShimmerLayout) {
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            if (!isPaused) {
                maskOffsetX =
                    (currentAnimationFromX + currentAnimationLength * value).toInt().toFloat()
                if (maskOffsetX + shimmerBitmapWidth >= 0) {
                    callback.draw()
                }
            }
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (finished)
                    return
                if (isPaused) {
                    animators!![currentAnimator].duration = newDuration
                    return
                }
                firstWait = false
                currentAnimator++
                if (currentAnimator == animators!!.size) {
                    callback.animationFinished()
                } else {
                    currentAnimationFromX = animationFromX!![currentAnimator].toFloat()
                    currentAnimationLength = animationLength!![currentAnimator].toFloat()
                    animators!![currentAnimator].start()
                }
            }
        })
    }

    //We create all the animators for a single row of the lrc file
    fun createAnimator(
        callback: IShimmerLayout,
        height: Int,
        paint: TextPaint,
        rows: List<LrcExtendedRow>,
        rowNumber: Int,
        shimmerColor: Int
    ) {
        val row = rows[rowNumber]
        maskRect = calculateBitmapMaskRect(paint, row, height)
        val width = maskRect.width()
        shimmerBitmapWidth = width

        maskBitmap = createBitmap(width, height)
        canvasForShimmerMask = Canvas(maskBitmap!!)

        createShimmerPaint(height, width, shimmerColor)

        if (row.content.isEmpty()) {
            //LRC Line non extended format

            if (rowNumber == 0) {
                val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
                animator.duration = row.initialTime
                addAnimatorListeners(animator, callback)
                animators!!.add(animator)
                animationFromX!!.add(0)
                animationLength!!.add(0)
                initialTime.add(0.toLong())
            } else {
                firstWait = false
            }

            val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
            if (rowNumber == rows.size - 1) {
                animator.duration = 1000
            } else {
                val nextRow = rows[rowNumber + 1]
                animator.duration = nextRow.initialTime - row.initialTime
            }
            initialTime.add(row.initialTime)

            addAnimatorListeners(animator, callback)
            val auxFromX = (-paint.measureText(row.fullSentence)).toInt()
            animators!!.add(animator)
            animationFromX!!.add(auxFromX)
            animationLength!!.add(-auxFromX)
            return
        }

        if (rowNumber == 0) {
            if (row.times[0] == 0L) {
                firstWait = false
            } else {
                val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
                animator.duration = row.times[0]
                addAnimatorListeners(animator, callback)
                animators!!.add(animator)
                animationFromX!!.add(0)
                animationLength!!.add(0)
                initialTime.add(0.toLong())
            }
        } else {
            val previousRow = rows[rowNumber - 1]
            if (previousRow.times.size === 0) {
                firstWait = false
            } else if (row.times[0].compareTo(previousRow.times[previousRow.times.size - 1]) <= 0) {
                firstWait = false
            } else {
                val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
                animator.duration = row.times[0] - previousRow.times[previousRow.times.size - 1]
                addAnimatorListeners(animator, callback)
                animators!!.add(animator)
                animationFromX!!.add(0)
                animationLength!!.add(0)
                initialTime.add(previousRow.times[previousRow.times.size - 1])
            }
        }

        var auxFromX = (-paint.measureText(row.fullSentence)).toInt()
        var auxToX = auxFromX + paint.measureText(row.content[0]).toInt()
        if (!row.times[0].equals(row.times[1])) {
            val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
            animator.duration = row.times[1] - row.times[0]
            addAnimatorListeners(animator, callback)
            animators!!.add(animator)
            animationFromX!!.add(auxFromX)
            animationLength!!.add(auxToX - auxFromX)
            initialTime.add(row.times[0])
        }

        for (i in 1 until row.content.size) {
            auxFromX = auxToX + paint.measureText(" ").toInt()
            auxToX = auxFromX + paint.measureText(row.content[i]).toInt()
            if (row.times[i].equals(row.times[i + 1])) {
                continue
            }
            val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
            animator.duration = row.times[i + 1] - row.times[i]
            addAnimatorListeners(animator, callback)
            animators!!.add(animator)
            animationFromX!!.add(auxFromX)
            animationLength!!.add(auxToX - auxFromX)
            initialTime.add(row.times[i])
        }
    }

    private fun calculateBitmapMaskRect(paint: TextPaint, row: LrcExtendedRow, height: Int): Rect {
        return Rect(0, 0, paint.measureText(row.fullSentence).toInt(), height)
    }

    private fun createBitmap(width: Int, height: Int): Bitmap? {
        try {
            return Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        } catch (e: OutOfMemoryError) {
            System.gc()
            return null
        }

    }

    private fun createShimmerPaint(height: Int, width: Int, shimmerColor: Int) {
        if (gradientTexturePaint != null) {
            return
        }

        val shimmerLineWidth = width / 2 * 0.99f

        val gradient = LinearGradient(
            0f, height.toFloat(),
            shimmerLineWidth.toInt().toFloat(),
            height.toFloat(),
            intArrayOf(shimmerColor, shimmerColor, shimmerColor, shimmerColor),
            floatArrayOf(0f, 0.5f - 0.99f / 2f, 0.5f + 0.99f / 2f, 1f),
            Shader.TileMode.CLAMP
        )

        val maskBitmapShader =
            BitmapShader(maskBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val composeShader = ComposeShader(gradient, maskBitmapShader, PorterDuff.Mode.DST_IN)

        gradientTexturePaint = Paint()
        gradientTexturePaint!!.isAntiAlias = true
        gradientTexturePaint!!.isDither = true
        gradientTexturePaint!!.isFilterBitmap = true
        gradientTexturePaint!!.shader = composeShader
    }

    fun animate() {
        if (animators!!.size != 0) {
            currentAnimationFromX = animationFromX!![0].toFloat()
            currentAnimationLength = animationLength!![0].toFloat()
            animators!![0].start()
        } else
            dispose()
    }

    //When we pause we decrease the remaining animator time accordingly.
    fun pause(currentTime: Long) {
        if (animators!!.size > currentAnimator) {
            val animator = animators!![currentAnimator]
            isPaused = true
            var progress = currentTime - initialTime[currentAnimator]
            if (progress < 0)
                progress = 0

            initialTime[currentAnimator] = currentTime
            newDuration = animator.duration - progress
            if (newDuration < 0)
                newDuration = 0
            var textProgress = currentAnimationLength * progress / animator.duration.toFloat()
            animator.end()
            if (textProgress > currentAnimationLength)
                textProgress = currentAnimationLength
            currentAnimationLength -= textProgress
            currentAnimationFromX += textProgress
        }
    }

    fun resume(callback: IShimmerLayout) {
        isPaused = false
        if (animators == null || currentAnimator == animators!!.size)
            callback.animationFinished()
        else
            animators!![currentAnimator].start()
    }

    fun checkEmpty(): Boolean {
        return if (animators!!.size == 0) true else false
    }

    fun reduceWait(wait: Long): Long {
        var wait = wait
        for (i in animators!!.indices) {
            val duration = animators!![i].duration
            if (wait < duration) {
                animators!![i].duration = duration - wait
                return 0
            } else {
                animators!![i].duration = 0
                wait -= duration
            }
        }
        return wait
    }

    fun dispose() {
        finished = true
        if (currentAnimator < animators!!.size) {
            val animator = animators!![currentAnimator]
            if (animator.isRunning)
                animator.end()
        }
        animators!!.clear()
        animationLength!!.clear()
        animationFromX!!.clear()
        animators = null
        animationLength = null
        animationFromX = null

        canvasForShimmerMask = null

        if (maskBitmap != null) {
            maskBitmap!!.recycle()
            maskBitmap = null
        }
    }
}
