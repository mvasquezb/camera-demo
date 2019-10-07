package com.example.camerademo.lrc

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import java.util.*

class ShimmerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    FrameLayout(context, attrs, defStyle), IShimmerLayout {

    private var maskRect: Rect? = null
    private var gradientTexturePaint: Paint? = null
    private var maskAnimator: ValueAnimator? = null

    private var localMaskBitmap: Bitmap? = null
    private var canvasForShimmerMask: Canvas? = null

    private var isAnimationStarted: Boolean = false
    private val autoStart: Boolean
    private val shimmerColor: Int

    private var rows: List<LrcExtendedRow>? = null
    private var currentRow: Int = 0
    private val rowAnimator = HashMap<Int, ShimmerAnimator>()
    private var currentAnimator: ShimmerAnimator? = null
    private var callback: ILrcPlayer? = null

    private var paint: TextPaint? = null
    private var previousLyric: TextView? = null
    private var nextLyric: TextView? = null
    private var highlightedLyric: TextView? = null

    private var extraTime: Long = 0

    private var startAnimationPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

    init {

        setWillNotDraw(false)
        shimmerColor = resources.getColor(android.R.color.darker_gray)
        autoStart = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!isAnimationStarted || width <= 0 || height <= 0 || currentAnimator!!.firstWait) {
            super.dispatchDraw(canvas)
        } else {
            dispatchDrawShimmer(canvas)
        }
    }

    private fun dispatchDrawShimmer(canvas: Canvas) {
        super.dispatchDraw(canvas)

        localMaskBitmap = currentAnimator!!.maskBitmap
        if (localMaskBitmap == null) {
            return
        }

        if (canvasForShimmerMask == null) {
            canvasForShimmerMask = Canvas(localMaskBitmap!!)
        }
        canvasForShimmerMask!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        canvasForShimmerMask!!.save()
        canvasForShimmerMask!!.translate(-currentAnimator!!.maskOffsetX, 0f)

        super.dispatchDraw(canvasForShimmerMask)

        canvasForShimmerMask!!.restore()

        drawShimmer(canvas)

        localMaskBitmap = null
    }

    private fun drawShimmer(destinationCanvas: Canvas) {
        destinationCanvas.save()

        destinationCanvas.translate(currentAnimator!!.maskOffsetX, 0f)
        destinationCanvas.drawRect(
            maskRect!!.left.toFloat(),
            0f,
            maskRect!!.width().toFloat(),
            maskRect!!.height().toFloat(),
            gradientTexturePaint!!
        )

        destinationCanvas.restore()
    }

    override fun draw() {
        invalidate()
    }

    override fun onDetachedFromWindow() {
        resetShimmering()
        super.onDetachedFromWindow()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.VISIBLE) {
            if (autoStart) {
                startShimmerAnimation()
            }
        } else {
            stopShimmerAnimation()
        }
    }

    override fun animationFinished() {

        rowAnimator.remove(currentRow)

        while (currentRow != rows!!.size) {
            currentRow++
            if (rowAnimator.containsKey(currentRow))
                break
        }

        if (currentRow == rows!!.size) {
            Log.i("Total Duration Row $currentRow", extraTime.toString())
            return
        }

        callback!!.newLine(currentRow)

        val realTime = callback!!.timePassed()
        extraTime = realTime - rows!![currentRow - 1].times[rows!![currentRow - 1].times.size - 1]

        setTextViews()
        currentAnimator = rowAnimator[currentRow]
        if (extraTime > 0) {
            currentAnimator!!.reduceWait(extraTime)
        }
        canvasForShimmerMask = currentAnimator!!.canvasForShimmerMask
        maskRect = currentAnimator!!.maskRect
        gradientTexturePaint = currentAnimator!!.gradientTexturePaint
        currentAnimator!!.animate()
    }

    private fun setTextViews() {
        if (currentRow != 0) {
            previousLyric!!.text = rows!![currentRow - 1].fullSentence
        }
        if (currentRow != rows!!.size - 1) {
            val row = rows!![currentRow + 1]
            nextLyric!!.text = row.fullSentence
            setCorrectTextColor(nextLyric!!, row)
        } else {
            nextLyric!!.text = ""
        }

        val row = rows!![currentRow]
        highlightedLyric!!.text = row.fullSentence
        setCorrectTextColor(highlightedLyric!!, row)
    }

    private fun setCorrectTextColor(textView: TextView, row: LrcExtendedRow) {
        textView.setTextColor(resources.getColor(android.R.color.holo_blue_light))
    }

    fun startShimmerAnimation() {
        if (isAnimationStarted) {
            return
        }

        if (width == 0) {
            startAnimationPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    startShimmerAnimation()

                    return true
                }
            }

            viewTreeObserver.addOnPreDrawListener(startAnimationPreDrawListener)

            return
        }

        currentAnimator = rowAnimator.get(0)
        canvasForShimmerMask = currentAnimator!!.canvasForShimmerMask
        maskRect = currentAnimator!!.maskRect
        gradientTexturePaint = currentAnimator!!.gradientTexturePaint
        isAnimationStarted = true
        currentAnimator!!.animate()
    }

    fun stopShimmerAnimation() {
        if (startAnimationPreDrawListener != null) {
            viewTreeObserver.removeOnPreDrawListener(startAnimationPreDrawListener)
        }

        resetShimmering()
    }

    private fun resetShimmering() {
        if (maskAnimator != null) {
            maskAnimator!!.removeAllUpdateListeners()
            maskAnimator!!.end()
        }

        maskAnimator = null
        gradientTexturePaint = null
        isAnimationStarted = false

        releaseBitMaps()
    }

    private fun releaseBitMaps() {
        canvasForShimmerMask = null
    }

    fun pause(currentTime: Long) {
        currentAnimator!!.pause(currentTime)
    }

    fun resume() {
        currentAnimator!!.resume(this)
    }

    fun restart() {
        resetShimmering()
        currentRow = 0
        isAnimationStarted = false
        for (animator in rowAnimator.values)
            animator.dispose()

        rowAnimator.clear()
        if (previousLyric != null)
            previousLyric!!.text = ""
        initialFill()
    }

    fun initialFill() {
        val amountCache = rows!!.size
        for (i in 0 until amountCache) {
            val animator = ShimmerAnimator()
            animator.createAnimator(
                this@ShimmerLayout, height, paint!!, rows!!, i, shimmerColor
            )
            if (animator.checkEmpty())
                continue
            synchronized(rowAnimator) {
                rowAnimator.put(i, animator)
            }
        }
        setTextViews()
    }

    fun finish() {
        resetShimmering()
        currentRow = 0
        isAnimationStarted = false
        for (animator in rowAnimator.values)
            animator.dispose()
        rowAnimator.clear()
    }

    fun setCallback(callback: ILrcPlayer) {
        this.callback = callback
    }

    fun setPaint(p: TextPaint) {
        paint = p
    }

    fun setLrc(rows: List<LrcExtendedRow>) {
        this.rows = rows
    }

    fun setPreviousLyric(previousLyric: TextView) {
        this.previousLyric = previousLyric
    }

    fun setNextLyric(nextLyric: TextView) {
        this.nextLyric = nextLyric
    }

    fun setHighlightedLyric(highlightedLyric: TextView) {
        this.highlightedLyric = highlightedLyric
    }
}