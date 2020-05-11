package tmg.labelledprogressbar

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

fun Float.dpToPx(context: Context): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
}

fun Int.dpToPx(context: Context): Float {
    return this.toFloat().dpToPx(context)
}

fun Float.pxToDp(context: Context): Float {
    return this / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Int.pxToDp(context: Context): Float {
    return this.toFloat().pxToDp(context)
}