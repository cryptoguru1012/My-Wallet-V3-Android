package piuk.blockchain.android.ui.tour

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import piuk.blockchain.android.R

import java.util.ArrayList

//
// Based on https://github.com/booncol/Pulsator4Droid
// Created by booncol on 04.07.2016.
//

@Suppress("MemberVisibilityCanBePrivate")
class PulseView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var repeat: Int = 0

    private var animatorSet: AnimatorSet? = null
    private var radius: Float = 0.toFloat()
    private var viewHeight: Float = 0.toFloat()
    private var centerX: Float = 0.toFloat()
    private var centerY: Float = 0.toFloat()

    private val defaultColor = ContextCompat.getColor(context, R.color.default_tour_pulse_color)

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val isStarted: Boolean
        @Synchronized get() = animatorSet?.isStarted ?: false

    // Number of pulses.
    var count: Int = 1
        set(count) {
            require(count > 0) { "Count must be positive and non-zero" }

            if (count != field) {
                field = count
                reset()
            }
        }

    // pulse duration.
    var duration: Long = DEFAULT_DURATION.toLong()
        set(millis) {
            require(millis > 0) { "Duration must be positive and non-zero" }
            if (millis != field) {
                field = millis
                reset()
            }
        }

    // Gets the current color of the pulse effect as RGB integer
    @ColorInt var color: Int = defaultColor
        set(color) {
            if (color != field) {
                field = color
                paint.color = color
            }
        }

    // Interpolator type used for animating.
    var interpolator: Int = DEFAULT_INTERPOLATOR
        set(type) {
            if (type != field) {
                field = type
                reset()
            }
        }

    init {
        val attr = context.theme.obtainStyledAttributes(
            attrs, R.styleable.PulseView, 0, 0
        )

        try {
            count = attr.getInteger(R.styleable.PulseView_pulse_count, DEFAULT_COUNT)
            duration = attr.getInteger(R.styleable.PulseView_pulse_duration, DEFAULT_DURATION).toLong()
            repeat = attr.getInteger(R.styleable.PulseView_pulse_repeat, DEFAULT_REPEAT)
            color = attr.getColor(R.styleable.PulseView_pulse_color, defaultColor)
            interpolator = attr.getInteger(R.styleable.PulseView_pulse_interpolator, DEFAULT_INTERPOLATOR)
        } finally {
            attr.recycle()
        }

        paint.color = this@PulseView.color

        build()
    }

    @Synchronized
    fun start() {
        if (!isStarted) {
            animatorSet?.start()
        }
    }

    @Synchronized
    fun stop() {
        if (isStarted) {
            animatorSet?.end()
        }
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val height = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        centerX = width * 0.5f
        centerY = height * 0.5f

        radius = centerX
        viewHeight = height.toFloat()

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun clear() {
        stop()
        removeAllViews()
    }

    private fun build() {
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        val repeats = if (repeat == INFINITE) ObjectAnimator.INFINITE else repeat

        val animators = ArrayList<Animator>()
        for (index in 0 until count) {
            val pulseView = PulseChildView(context).apply {
                scaleX = 0f
                scaleY = 0f
                alpha = 1f

                addView(this, index, layoutParams)
            }

            val delay = (index * duration / count)

            ObjectAnimator.ofFloat(pulseView, "ScaleX", 0f, 1f).apply {
                repeatCount = repeats
                repeatMode = ObjectAnimator.RESTART
                startDelay = delay
                animators.add(this)
            }

            ObjectAnimator.ofFloat(pulseView, "ScaleY", 0f, 1f).apply {
                repeatCount = repeats
                repeatMode = ObjectAnimator.RESTART
                startDelay = delay
                animators.add(this)
            }

            ObjectAnimator.ofFloat(pulseView, "Alpha", 0.5f, 0f).apply {
                repeatCount = repeats
                repeatMode = ObjectAnimator.RESTART
                startDelay = delay
                animators.add(this)
            }
        }

        animatorSet = AnimatorSet().apply {
            playTogether(animators)
            interpolator = createInterpolator(this@PulseView.interpolator)
            duration = this@PulseView.duration
        }
    }

    private fun reset() {
        val isStarted = isStarted

        clear()
        build()

        if (isStarted) {
            start()
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        animatorSet?.cancel()
        animatorSet = null
    }

    private inner class PulseChildView(context: Context) : View(context) {

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }

    companion object {
        private const val INFINITE = 0

        private const val INTERPOLATOR_LINEAR = 0
        private const val INTERPOLATOR_ACCELERATE = 1
        private const val INTERPOLATOR_DECELERATE = 2
        private const val INTERPOLATOR_ACCELERATE_DECELERATE = 3

        private const val DEFAULT_COUNT = 4
        private const val DEFAULT_DURATION = 7000
        private const val DEFAULT_REPEAT = INFINITE
        private const val DEFAULT_INTERPOLATOR = INTERPOLATOR_LINEAR

        private fun createInterpolator(type: Int): Interpolator {
            return when (type) {
                INTERPOLATOR_ACCELERATE -> AccelerateInterpolator()
                INTERPOLATOR_DECELERATE -> DecelerateInterpolator()
                INTERPOLATOR_ACCELERATE_DECELERATE -> AccelerateDecelerateInterpolator()
                else -> LinearInterpolator()
            }
        }
    }
}
