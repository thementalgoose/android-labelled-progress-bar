package tmg.labelledprogressbar.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_progress.*

class ProgressSampleActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        progressBar.animateProgress(0.75f)

        val startValue: Float = 500f
        val endValue: Float = 1500f

        progressBar2.timeLimit = 5000
        progressBar2.animateProgress(0.8f) {
            (startValue + ((endValue - startValue) * it)).toInt().toString()
        }
    }
}