
package com.justdoit.arcmenu

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.RelativeLayout
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.arc_menu.view.*
import java.util.concurrent.TimeUnit


class ArcMenu @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    var onControl: (() -> Unit)? = null

    init {
        View.inflate(context, R.layout.arc_menu, this)

        controlLayout.isClickable = true
        RxView.clicks(controlLayout)
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribe(object : CommonObserver<Any>() {
                override fun onNext(o: Any) {
                    onControl?.invoke()

                    postDelayed({
                        hintView.startAnimation(createHintSwitchAnimation(arcLayout.isExpanded))
                        arcLayout.switchState(true, 0)
                    }, 100)
                }

                override fun onError(e: Throwable) {

                }
            })

        applyAttrs(attrs)
    }

    private fun applyAttrs(attrs: AttributeSet?) {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.ArcLayout, 0, 0).apply {
                val fromDegrees = getFloat(R.styleable.ArcLayout_fromDegrees, ArcLayout.DEFAULT_FROM_DEGREES)
                val toDegrees = getFloat(R.styleable.ArcLayout_toDegrees, ArcLayout.DEFAULT_TO_DEGREES)
                arcLayout.setArc(fromDegrees, toDegrees)

                val defaultChildSize = arcLayout.childSize
                val newChildSize = getDimensionPixelSize(R.styleable.ArcLayout_childSize, defaultChildSize)
                arcLayout.childSize = newChildSize

                recycle()
            }
        }
    }

    fun addItem(item: View, onItemClick: () -> Unit) {
        arcLayout.addView(item)
        item.setOnClickListener{
            onItemClick.invoke()
            packUpMenu()
        }
    }

    fun packUpMenu() {
        postDelayed({
            hintView.startAnimation(createHintSwitchAnimation(true))
            arcLayout.switchState(true, 1)
        }, 100)
    }

    private fun createHintSwitchAnimation(expanded: Boolean): Animation {
        return RotateAnimation((if (expanded) 45 else 0).toFloat(), (if (expanded) 0 else 45).toFloat(), Animation.RELATIVE_TO_SELF,
            0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            startOffset = 0
            duration = 100
            interpolator = DecelerateInterpolator()
            fillAfter = true
        }
    }
}
