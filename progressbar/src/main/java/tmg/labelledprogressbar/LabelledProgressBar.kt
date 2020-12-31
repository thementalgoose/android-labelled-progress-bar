package tmg.labelledprogressbar

import android.animation.Animator
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

private const val defaultRadius: Float = 0f
private const val defaultSliverWidth: Float = 0f
private const val defaultTextSize: Float = 16f

private val defaultResolver: ((progress: Float) -> String) = { "${(it * 100).toInt()}%" }
private val defaultEvaluator: LabelledProgressBarEvaluator = object : LabelledProgressBarEvaluator {
    override fun evaluate(progress: Float) = defaultResolver(progress)
}

class LabelledProgressBar : View, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

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
    @Deprecated("Specify the width using `sliverWidthDp`. Set width to zero to turn it off")
    var showSliverOnZero: Boolean
        get() = sliverWidthDp != 0f
        set(value) = when (value) {
            true -> sliverWidthDp = defaultSliverWidth
            false -> sliverWidthDp = 0f
        }

    /**
     * Show a small amount of the progress bar when the value is zero
     *
     * If this value is set to 0, the sliver is not shown
     */
    var sliverWidthDp: Float
        get() = sliverWidth.pxToDp(context)
        set(value) {
            sliverWidth = sliverWidthDp.dpToPx(context)
        }

    /**
     * Box radius of the progress bar
     */
    var radiusDp: Float
        get() = radius.pxToDp(context)
        set(value) {
            radius = radiusDp.dpToPx(context)
        }

    /**
     * If the animation should start from.
     *   true = animates from left to right
     *   false = animated from right to left
     * Defaults to true (LTR)
     */
    var fromLeft: Boolean = true

    private var radius: Float = defaultRadius.dpToPx(context)
    private var sliverWidth: Float = defaultSliverWidth.dpToPx(context)

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
    private var textBarPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    private var textBackgroundPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
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
                    sliverWidth = getDimension(R.styleable.LabelledProgressBar_lpb_sliverWidth, defaultSliverWidth)
                    showSliverOnZero = getBoolean(R.styleable.LabelledProgressBar_lpb_showSliverOnEmpty, false)
                    radius = getDimension(R.styleable.LabelledProgressBar_lpb_radius, radius)

                    fromLeft = getBoolean(R.styleable.LabelledProgressBar_lpb_fromLeft, true)

                    initialProgress = getFloat(R.styleable.LabelledProgressBar_lpb_initialProgress, initialProgress).coerceIn(0f, 1f)
                    initialAnimate = getBoolean(R.styleable.LabelledProgressBar_lpb_initialAnimate, initialAnimate)

                    // To be removed in the future
                    if (showSliverOnZero && sliverWidth == 0.0f) {
                        if (BuildConfig.DEBUG) {
                            Log.d("LabelledProgressBar", "showSliverOnEmpty is set to true but has been deprecated, setting sliverWidth to 1dp. Please use sliverWidth to customise this")
                        }
                        sliverWidth = defaultSliverWidth
                    }
                } finally {
                    recycle()
                }
            }

        if (initialProgress != 0f) {
            if (initialAnimate) {
                startAnimation(initialProgress)
            } else {
                progressPercentage = initialProgress
            }
            maxPercentage = initialProgress
        }
    }

    //region Public methods for starting / setting progress

    fun setProgress(progress: Float, evaluator: LabelledProgressBarEvaluator) {
        this.labelResolver = evaluator
        firstRun = true
        maxPercentage = progress.coerceIn(0.0f, 1.0f)
        progressPercentage = progress.coerceIn(0.0f, 1.0f)
        updateContentDescription()
        invalidate()
    }

    fun setProgress(progress: Float, resolver: ((progress: Float) -> String) = defaultResolver) {
        this.setProgress(progress, object : LabelledProgressBarEvaluator {
            override fun evaluate(progress: Float) = resolver.invoke(progress)
        })
    }

    fun animateProgress(
        progress: Float,
        fromBeginning: Boolean = true,
        evaluator: LabelledProgressBarEvaluator
    ) {
        // fromBeginning = false will mean the bar will animate from it's last position (for first run)
        this.firstRun = true
        this.labelResolver = evaluator
        if (fromBeginning) {
            progressPercentage = 0f
        }
        maxPercentage = progress.coerceIn(0.0f, 1.0f)
        startAnimation(progress.coerceIn(0.0f, 1.0f))
        updateContentDescription()
    }

    fun animateProgress(
        progress: Float,
        fromBeginning: Boolean = true,
        resolver: ((progress: Float) -> String) = defaultResolver
    ) {
        // fromBeginning = false will mean the bar will animate from it's last position (for first run)
        this.animateProgress(progress, fromBeginning, object : LabelledProgressBarEvaluator {
            override fun evaluate(progress: Float): String = resolver.invoke(progress)
        })
    }

    //endregion

    private fun updateContentDescription() {
        val maxLabel = this.labelResolver.evaluate(maxPercentage)
        contentDescription = if (maxLabel.trim().isNotEmpty()) {
            maxLabel
        } else {
            "${(maxPercentage * 100f).toInt()}%"
        }
    }

    private fun initVariables() {
        canvasWidth = width.toFloat()
        canvasHeight = height.toFloat()

        textY = (canvasHeight / 2f) + textBarPaint.fontMetrics.bottom

        firstRun = false
    }

    private fun shouldDrawOnBar(maxPercentage: Float) {
        val textWidth =
            textBarPaint.measureText(labelResolver.evaluate(maxPercentage)) + (textPadding * 2f)
        val barFinalWidth = maxPercentage * canvasWidth
        drawOnBar = textWidth < barFinalWidth
    }

    private fun startAnimation(withProgress: Float) {
        valueAnimator = ValueAnimator.ofFloat(progressPercentage, withProgress)
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = (timeLimit.toLong())
        valueAnimator.addListener(this)
        valueAnimator.addUpdateListener(this)
        valueAnimator.start()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (firstRun) {
            initVariables()
            shouldDrawOnBar(maxPercentage)
        }

        var progressPercentageToDraw = when (sliverWidth == 0f) {
            true -> progressPercentage
            false -> if (maxPercentage == 0.0f) sliverWidth / canvasWidth else progressPercentage
        }

        if (isInEditMode) {
            if (initialProgress != 0f) {
                progressPercentageToDraw = initialProgress
            }
            if (progressPercentageToDraw == 0f) {
                progressPercentageToDraw = 0.5f
            }
        }

        val percentage: String = labelResolver.evaluate(progressPercentageToDraw)
        val textWidth: Float = textBarPaint.measureText(percentage)

        canvas?.apply {

            // Draw background
            drawRoundRect(0f, 0f, canvasWidth, canvasHeight, radius, radius, backgroundPaint)

            // Draw prorgess bar
            drawProgressBar(fromLeft, progressPercentageToDraw)

            // Draw text
            when {
                fromLeft && drawOnBar -> {
                    drawText(percentage, (progressPercentageToDraw * canvasWidth) - textPadding - textWidth, textY, textBarPaint)
                }
                fromLeft && !drawOnBar -> {
                    drawText(percentage, (progressPercentageToDraw * canvasWidth) + textPadding, textY, textBackgroundPaint)
                }
                !fromLeft && drawOnBar -> {
                    drawText(percentage, ((1.0f - progressPercentageToDraw) * canvasWidth) + textPadding, textY, textBarPaint)
                }
                !fromLeft && !drawOnBar -> {
                    drawText(percentage, ((1.0f - progressPercentageToDraw) * canvasWidth) - (textPadding + textWidth), textY, textBackgroundPaint)
                }
            }
        }
    }

    private fun Canvas.drawProgressBar(fromLeft: Boolean, progressPercentageToDraw: Float) {
        if (fromLeft) {
            drawRoundRect(0f, 0f, progressPercentageToDraw * canvasWidth, canvasHeight, radius, radius, progressPaint)
        }
        else {
            drawRoundRect(canvasWidth, 0f, (1.0f - progressPercentageToDraw) * canvasWidth, canvasHeight, radius, radius, progressPaint)
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

    //region Animator.AnimatorListener

    override fun onAnimationEnd(animation: Animator?) {
        progressPercentage = maxPercentage
        invalidate()
    }

    override fun onAnimationStart(animation: Animator?) { /* Do nothing */ }
    override fun onAnimationCancel(animation: Animator?) { /* Do nothing */ }
    override fun onAnimationRepeat(animation: Animator?) { /* Do nothing */ }

    //endregion
}