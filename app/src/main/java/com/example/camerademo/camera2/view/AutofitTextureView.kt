package com.example.camerademo.camera2.view

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import kotlin.math.abs

open class AutofitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {
    private var mTargetAspect = -1.0        // initially use default window size

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        val newRatio = width.toDouble() / height
        if (mTargetAspect != newRatio) {
            mTargetAspect = newRatio
            requestLayout()
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var newWidthMeasureSpec = widthMeasureSpec
        var newHeightMeasureSpec = heightMeasureSpec
        // Target aspect ratio will be < 0 if it hasn't been set yet.  In that case,
        // we just use whatever we've been handed.
        if (mTargetAspect > 0) {
            var initialWidth = MeasureSpec.getSize(widthMeasureSpec)
            var initialHeight = MeasureSpec.getSize(heightMeasureSpec)

            // factor the padding out
            val horizPadding = paddingLeft + paddingRight
            val vertPadding = paddingTop + paddingBottom
            initialWidth -= horizPadding
            initialHeight -= vertPadding

            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = mTargetAspect / viewAspectRatio - 1

            if (abs(aspectDiff) < 0.01) {
                // We're very close already.  We don't want to risk switching from e.g. non-scaled
                // 1280x720 to scaled 1280x719 because of some floating-point round-off error,
                // so if we're really close just leave it alone.
                /*Log.d(TAG, "aspect ratio is good (target=" + mTargetAspect +
                        ", view=" + initialWidth + "x" + initialHeight + ")");*/
            } else {
                if (aspectDiff > 0) {
                    // limited by narrow width; restrict height
                    initialHeight = (initialWidth / mTargetAspect).toInt()
                } else {
                    // limited by short height; restrict width
                    initialWidth = (initialHeight * mTargetAspect).toInt()
                }
                /*Log.d(TAG, "new size=" + initialWidth + "x" + initialHeight + " + padding " +
                        horizPadding + "x" + vertPadding);*/
                initialWidth += horizPadding
                initialHeight += vertPadding
                newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
            }
        }

        //Log.d(TAG, "set width=[" + MeasureSpec.toString(widthMeasureSpec) +
        //        "] height=[" + View.MeasureSpec.toString(heightMeasureSpec) + "]");
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
    }
}