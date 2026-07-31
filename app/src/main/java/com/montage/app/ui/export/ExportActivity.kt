package com.montage.app.ui.export

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.montage.app.R
import com.montage.app.engine.ExportEngine
import kotlinx.coroutines.launch
import java.io.File

class ExportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        val videoUri = intent.getStringExtra("video_uri") ?: return
        val resolution = intent.getStringExtra("resolution") ?: "1080p"
        val fps = intent.getIntExtra("fps", 30)

        val (width, height) = getResolution(resolution)

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "M-edit_${System.currentTimeMillis()}.mp4"
        )

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val progressText = findViewById<TextView>(R.id.progressText)
        val statusText = findViewById<TextView>(R.id.statusText)

        lifecycleScope.launch {
            val success = ExportEngine.export(
                context = this@ExportActivity,
                inputUri = Uri.parse(videoUri),
                outputFile = outputFile,
                targetWidth = width,
                targetHeight = height,
                fps = fps,
                bitrate = calculateBitrate(width, height),
                onProgress = { progress ->
                    runOnUiThread {
                        progressBar.progress = progress
                        progressText.text = "$progress%"
                    }
                }
            )
            runOnUiThread {
                if (success) {
                    statusText.text = "تم التصدير بنجاح: ${outputFile.absolutePath}"
                } else {
                    statusText.text = "فشل التصدير"
                }
            }
        }
    }

    private fun getResolution(res: String): Pair<Int, Int> {
        return when (res) {
            "4k" -> 3840 to 2160
            "2k" -> 2560 to 1440
            else -> 1920 to 1080
        }
    }

    private fun calculateBitrate(width: Int, height: Int): Int {
        // للمحافظة على جودة عالية: استخدم bitrate نسبي
        return (width * height * 30 * 0.15).toInt().coerceAtLeast(8_000_000)
    }
}
