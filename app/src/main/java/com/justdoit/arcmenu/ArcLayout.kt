package com.justdoit.arcmenu

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.view.animation.Animation.AnimationListener

class ArcLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {

        const val DEFAULT_FROM_DEGREES = 270.0f

        const val DEFAULT_TO_DEGREES = 360.0f
    }

    init {
        attrs?.let {
            getContext().obtainStyledAttributes(it, R.styleable.ArcLayout, 0, 0).apply {
                mFromDegrees = getFloat(R.styleable.ArcLayout_fromDegrees, DEFAULT_FROM_DEGREES)
                mToDegrees = getFloat(R.styleable.ArcLayout_toDegrees, DEFAULT_TO_DEGREES)
                mChildSize = Math.max(getDimensionPixelSize(R.styleable.ArcLayout_childSize, 0), 0)
                recycle()
            }
        }
    }

    private var mChildSize: Int = 0

    private val mChildPadding = 5

    private val mLayoutPadding = 10

    private var mFromDegrees = DEFAULT_FROM_DEGREES

    private var mToDegrees = DEFAULT_TO_DEGREES

    private var mRadius: Int = 0

    private val mAccelerateInterpolator by lazy { AccelerateInterpolator() }

    private val mOvershootInterpolator by lazy { OvershootInterpolator(1.0f) }

    var isExpanded = false
        private set

    private var isRunning: Boolean = false

    var childSize: Int
        get() = mChildSize
        set(size) {
            if (mChildSize == size || size < 0) {
                return
            }

            mChildSize = size

            requestLayout()
        }

    private fun computeRadius(arcDegrees: Float, childCount: Int, childSize: Int,
                              childPadding: Int, minRadius: Int): Int {
        if (childCount < 2) {
            return minRadius
        }

        val perDegrees = arcDegrees / (childCount - 1)
        val perHalfDegrees = perDegrees / 2
        val perSize = childSize + childPadding

        val radius = (perSize / 2 / Math.sin(Math.toRadians(perHalfDegrees.toDouble()))).toInt()

        return Math.max(radius, minRadius)
    }

    private fun computeChildFrame(centerX: Int, centerY: Int, radius: Int, degrees: Float,
                                  size: Int): Rect {

        val childCenterX = centerX + radius * Math.cos(Math.toRadians(degrees.toDouble()))
        val childCenterY = centerY + radius * Math.sin(Math.toRadians(degrees.toDouble()))

        return Rect((childCenterX - size / 2).toInt(), (childCenterY - size / 2).toInt(),
            (childCenterX + size / 2).toInt(), (childCenterY + size / 2).toInt())
    }

    private fun computeStartOffset(childCount: Int, expanded: Boolean, index: Int,
                                   delayPercent: Float, duration: Long, interpolator: Interpolator): Long {
        val delay = delayPercent * duration
        val viewDelay = (getTransformedIndex(expanded, childCount, index) * delay).toLong()
        val totalDelay = delay * childCount

        var normalizedDelay = viewDelay / totalDelay
        normalizedDelay = interpolator.getInterpolation(normalizedDelay)

        return (normalizedDelay * totalDelay).toLong()
    }

    private fun getTransformedIndex(expanded: Boolean, count: Int, index: Int): Int {
        return if (expanded) {
            count - 1 - index
        } else index

    }

    private fun createExpandAnimation(fromXDelta: Float, toXDelta: Float, fromYDelta: Float, toYDelta: Float,
                                      startOffset: Long, duration: Long, interpolator: Interpolator): Animation {
        return RotateAndTranslateAnimation(0f, toXDelta, 0f, toYDelta, 0f, 720f).apply {
            this.startOffset = startOffset
            this.duration = duration
            this.interpolator = interpolator
            fillAfter = true
        }
    }

    private fun createShrinkAnimation(fromXDelta: Float, toXDelta: Float, fromYDelta: Float, toYDelta: Float,
                                      startOffset: Long, duration: Long, interpolator: Interpolator): Animation {
        return AnimationSet(false).apply {

            fillAfter = true

            val preDuration = duration / 2
            RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.startOffset = startOffset
                this.duration = preDuration
                //        rotateAnimation.setInterpolator(new LinearInterpolator());
                this.fillAfter = true

                addAnimation(this)
            }

            RotateAndTranslateAnimation(0f, toXDelta, 0f, toYDelta, 360f, 720f).apply {
                this.startOffset = startOffset + preDuration
                this.duration = duration - preDuration
                //        translateAnimation.setInterpolator(interpolator);
                this.fillAfter = true

                addAnimation(this)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        mRadius = computeRadius(Math.abs(mToDegrees - mFromDegrees), childCount, mChildSize,
            mChildPadding, resources.getDimension(R.dimen.dp750_200).toInt())
        val radius = mRadius
        val size = radius * 2 + mChildSize + mChildPadding + mLayoutPadding * 2

        setMeasuredDimension(size, size)

        val count = childCount
        for (i in 0 until count) {
            getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(mChildSize, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mChildSize, View.MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (isRunning) {
            return
        }
        val centerX = width / 2
        val centerY = height / 2
        val radius = if (isExpanded) mRadius else 0

        val childCount = childCount
        val perDegrees = (mToDegrees - mFromDegrees) / (childCount - 1)

        var degrees = mFromDegrees
        for (i in 0 until childCount) {
            val frame = computeChildFrame(centerX, centerY, radius, degrees, mChildSize)
            degrees += perDegrees
            getChildAt(i).layout(frame.left, frame.top, frame.right, frame.bottom)
        }
    }

    private fun bindChildAnimation(child: View, index: Int, duration: Long) {
        val expanded = isExpanded
        val centerX = width / 2
        val centerY = height / 2
        val radius = if (expanded) 0 else mRadius

        val childCount = childCount
        val perDegrees = (mToDegrees - mFromDegrees) / (childCount - 1)
        val frame = computeChildFrame(centerX, centerY, radius, mFromDegrees + index * perDegrees, mChildSize)

        val toXDelta = frame.left - child.left
        val toYDelta = frame.top - child.top

        val startOffset = computeStartOffset(childCount, isExpanded, index, 0.1f, duration, mAccelerateInterpolator)

        val animation = if (isExpanded)
            createShrinkAnimation(0f, toXDelta.toFloat(), 0f, toYDelta.toFloat(), startOffset, duration,
                mAccelerateInterpolator)
        else
            createExpandAnimation(0f, toXDelta.toFloat(), 0f, toYDelta.toFloat(), startOffset, duration, mOvershootInterpolator)

        val isLast = getTransformedIndex(expanded, childCount, index) == childCount - 1
        animation.setAnimationListener(object : AnimationListener {

            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                if (isLast) {
                    postDelayed({ onAllAnimationsEnd() }, 0)
                }
            }
        })

        child.animation = animation
    }

    fun setArc(fromDegrees: Float, toDegrees: Float) {
        if (mFromDegrees == fromDegrees && mToDegrees == toDegrees) {
            return
        }

        mFromDegrees = fromDegrees
        mToDegrees = toDegrees

        requestLayout()
    }

    /**
     * 展开和收缩切换
     */
    fun switchState(showAnimation: Boolean, type: Int) {
        isRunning = true

        if (showAnimation) {
            val childCount = childCount
            for (i in 0 until childCount) {
                bindChildAnimation(getChildAt(i), i, 300)
            }
        }

        isExpanded = type == 0 && !isExpanded

        if (!showAnimation) {
            requestLayout()
        }

        invalidate()
    }

    private fun onAllAnimationsEnd() {
        val childCount = childCount
        for (i in 0 until childCount) {
            getChildAt(i).clearAnimation()
        }
        isRunning = false
        requestLayout()
    }
}
