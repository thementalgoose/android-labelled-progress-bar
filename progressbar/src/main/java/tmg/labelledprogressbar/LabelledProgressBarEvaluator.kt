package tmg.labelledprogressbar

interface LabelledProgressBarEvaluator {
    fun evaluate(progress: Float): String
}