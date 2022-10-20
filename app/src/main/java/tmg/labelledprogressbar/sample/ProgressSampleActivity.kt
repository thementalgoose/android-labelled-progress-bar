package tmg.labelledprogressbar.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tmg.labelledprogressbar.sample.databinding.ActivityProgressBinding

class ProgressSampleActivity: AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.animateProgress(0.75f)

        val startValue: Float = 500f
        val endValue: Float = 1500f

        binding.progressBar2.timeLimit = 5000
        binding.progressBar2.animateProgress(0.8f) {
            (startValue + ((endValue - startValue) * it)).toInt().toString()
        }

        binding.progressBar6.animateProgress(1.0f) {
            when {
                it <= 0.2f -> "poor"
                it <= 0.4f -> "bad"
                it <= 0.6f -> "average"
                it <= 0.8f -> "good"
                else -> "excellent"
            }
        }

        binding.progressBar7.animateProgress(0.0f) {
            "0 - No sliver"
        }
        binding.progressBar8.animateProgress(0.0f) {
            "0 - Sliver shown"
        }
    }
}