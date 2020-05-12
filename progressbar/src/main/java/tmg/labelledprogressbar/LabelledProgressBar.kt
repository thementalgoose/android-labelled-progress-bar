package tmg.labelledprogressbar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.min

private const val defaultTextPaddingDp: Float = 8f
private const val defaultBackgroundColor: Int = Color.WHITE
private const val defaultProgressColor: Int = Color.CYAN
private const val defaultTextBarColor: Int = Color.WHITE
private const val defaultTextBackgroundColor: Int = Color.BLACK

private const val defaultTimeLimit: Long = 1000L
private const val defaultShowSliverOnZero: Boolean = true

private const val defaultRadius: Float = 0f
private const val defaultTextSize: Float = 16f

private val defaultResolver: ((progress: Float) -> String) = { "${(it * 100).toInt()}%" }
private val defaultEvaluator: LabelledProgressBarEvaluator = object : LabelledProgressBarEvaluator {
    override fun evaluate(progress: Float) = defaultResolver(progress)
}

/**
 * Copied from initial implementation as CorrectPercentProgressView from Transmission
 */
class LabelledProgressBar : View, ValueAnimator.AnimatorUpdateListener {

    /**
     * Background color of the progress var
     */
    var backgroundColour: Int = defaultBackgroundColor
        set(value) {
            backgroundPaint.color = value
            field = value
        }
    /**
     * Background color of the progress bar
     */
    var progressColour: Int = defaultProgressColor
        set(value) {
            progressPaint.color = value
            field = value
        }

    /**
     * Text colour when the text is displayed on the progress bar
     * (ie. On the left hand side of the progress bar)
     */
    var textBarColour: Int = defaultTextBarColor
        set(value) {
            textBarPaint.color = value
            field = value
        }

    /**
     * Text colour when the text is displayed on the background
     * (ie. On the right hand side of the progress bar)
     */
    var textBackgroundColour: Int = defaultTextBackgroundColor
        set(value) {
            textBackgroundPaint.color = value
            field = value
        }

    /**
     * Time limit for the animation of the bar sliding in
     * Defaults to 1000 (1 second)
     */
    var timeLimit: Int = defaultTimeLimit.toInt()

    /**
     * Text size of the label that's displayed
     */
    var textSizeSp: Float
        get() = textSize.pxToDp(context)
        set(value) {
            textSize = textSizeSp.dpToPx(context)
        }
    private var textSize: Float = defaultTextSize.dpToPx(context)
        set(value) {
            textBarPaint.textSize = value
            textBackgroundPaint.textSize = value
            field = value
        }

    /**
     * Show a small amount of the progress bar when the value is zero
     *
     * true: A small amount is displayed on the left
     * false: Bar is completely empty when progress is 0
     */
    var showSliverOnZero: Boolean = defaultShowSliverOnZero

    /**
     * Box radius of the progress bar
     */
    var radiusDp: Float
        get() = radius.pxToDp(context)
        set(value) {
            radius = radiusDp.dpToPx(context)
        }

    private var radius: Float = defaultRadius.dpToPx(context)

    private var maxPercentage: Float = 0.0f
    private var drawOnBar: Boolean = true
    private var textPadding: Float = defaultTextPaddingDp.dpToPx(context)
    private var textY: Float = 0f

    private var canvasWidth: Float = 0.0f
    private var canvasHeight: Float = 0.0f
    private var progressPercentage: Float = 0.0f
    private var firstRun: Boolean = true
    private var backgroundPaint: Paint = Paint()
    private var progressPaint: Paint = Paint()
    private var textBarPaint: Paint = Paint()
    private var textBackgroundPaint: Paint = Paint()
    private lateinit var valueAnimator: ValueAnimator
    private var labelResolver: LabelledProgressBarEvaluator = defaultEvaluator

    private var initialProgress: Float = 0.0f
    private var initialAnimate: Boolean = true

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initView(attrs, defStyleAttr)
    }

    private fun initView(attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) {
        context
            .theme
            .obtainStyledAttributes(attributeSet, R.styleable.LabelledProgressBar, defStyleAttr, 0)
            .apply {
                try {
                    backgroundColour = getColor(R.styleable.LabelledProgressBar_lpb_backgroundColour, defaultBackgroundColor)
                    progressColour = getColor(R.styleable.LabelledProgressBar_lpb_progressColour, defaultProgressColor)
                    textBarColour = getColor(R.styleable.LabelledProgressBar_lpb_textBarColour, textBarColour)
                    textBackgroundColour = getColor(R.styleable.LabelledProgressBar_lpb_textBackgroundColour, textBackgroundColour)
                    textPadding = getDimension(R.styleable.LabelledProgressBar_lpb_textPadding, textPadding)
                    textSize = getDimension(R.styleable.LabelledProgressBar_lpb_textSize, textSize)
                    timeLimit = getInt(R.styleable.LabelledProgressBar_lpb_timeLimit, timeLimit)
                    showSliverOnZero = getBoolean(R.styleable.LabelledProgressBar_lpb_showSliverOnEmpty, defaultShowSliverOnZero)
                    radius = getDimension(R.styleable.LabelledProgressBar_lpb_radius, radius)

                    initialProgress = getFloat(R.styleable.LabelledProgressBar_lpb_initialProgress, initialProgress).coerceIn(0f, 1f)
                    initialAnimate = getBoolean(R.styleable.LabelledProgressBar_lpb_initialAnimate, initialAnimate)
                } finally {
                    recycle()
                }
            }

        if (initialProgress != 0f) {
            if (initialAnimate) {
                start(initialProgress)
            }
            else {
                progressPercentage = initialProgress
            }
            maxPercentage = initialProgress
        }
    }

    //region Public methods for call

    fun setProgress(progress: Float, evaluator: LabelledProgressBarEvaluator) {
        this.labelResolver = evaluator
        maxPercentage = progress.coerceIn(0.0f, 1.0f)
        progressPercentage = progress.coerceIn(0.0f, 1.0f)
        drawOnBar(maxPercentage)
    }

    fun setProgress(progress: Float, resolver: ((progress: Float) -> String) = defaultResolver) {
        this.setProgress(progress, object : LabelledProgressBarEvaluator {
            override fun evaluate(progress: Float) = resolver.invoke(progress)
        })
    }

    fun animateProgress(progress: Float, fromLastPosition: Boolean = true, evaluator: LabelledProgressBarEvaluator) {
        this.labelResolver = evaluator
        if (fromLastPosition) {
            progressPercentage = 0f
        }
        maxPercentage = progress.coerceIn(0.0f, 1.0f)
        start(progress.coerceIn(0.0f, 1.0f))
        drawOnBar(maxPercentage)
    }

    fun animateProgress(progress: Float, fromLastPosition: Boolean = true, resolver: ((progress: Float) -> String) = defaultResolver) {
        this.animateProgress(progress, fromLastPosition, object : LabelledProgressBarEvaluator {
            override fun evaluate(progress: Float): String = resolver.invoke(progress)
        })
    }

    //endregion

    private fun initVariables() {
        canvasWidth = width.toFloat()
        canvasHeight = height.toFloat()

        textY = (canvasHeight / 2f) + textBarPaint.fontMetrics.bottom

        firstRun = false
    }

    private fun drawOnBar(maxPercentage: Float) {
        val textWidth = textBarPaint.measureText(labelResolver.evaluate(maxPercentage)) + (textPadding * 2f)
        val barFinalWidth = maxPercentage * canvasWidth
        drawOnBar = textWidth < barFinalWidth
    }

    private fun start(withProgress: Float) {
        val prog: Float = max(if (showSliverOnZero) 0.01f else 0.0f, min(1.0f, withProgress))
        valueAnimator = ValueAnimator.ofFloat(progressPercentage, prog)
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = (timeLimit.toLong())
        valueAnimator.addUpdateListener(this)
        valueAnimator.start()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (firstRun) {
            initVariables()
            drawOnBar(maxPercentage)
        }

        if (isInEditMode) {
            if (initialProgress != 0f) {
                progressPercentage = initialProgress
            }
            if (progressPercentage == 0f) {
                progressPercentage = 0.5f
            }
        }

        val percentage: String = labelResolver.evaluate(progressPercentage)
        val textWidth: Float = textBarPaint.measureText(percentage)

        canvas?.drawRoundRect(
            0f,
            0f,
            canvasWidth,
            canvasHeight,
            radius,
            radius,
            backgroundPaint
        )
        canvas?.drawRoundRect(
            0f,
            0f,
            progressPercentage * canvasWidth,
            canvasHeight,
            radius,
            radius,
            progressPaint
        )

        if (drawOnBar) {
            canvas?.drawText(percentage, (progressPercentage * canvasWidth) - textPadding - textWidth, textY, textBarPaint)
        } else {
            canvas?.drawText(percentage,(progressPercentage * canvasWidth) + textPadding, textY, textBackgroundPaint)
        }
    }

    //region ValueAnimator.AnimatorUpdateListener

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        if (animation == valueAnimator) {
            progressPercentage = animation.animatedValue as Float
            invalidate()
        }
    }

    //endregion
}