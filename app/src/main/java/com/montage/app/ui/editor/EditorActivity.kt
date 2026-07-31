package com.montage.app.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.RangeSlider
import com.montage.app.R
import com.montage.app.data.db.AppDatabase
import com.montage.app.ui.export.ExportSettingsActivity
import kotlinx.coroutines.*

class EditorActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var rangeSlider: RangeSlider
    private var videoUri: Uri? = null
    private var projectId: Long = -1
    private var videoDuration: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        videoView = findViewById(R.id.videoView)
        rangeSlider = findViewById(R.id.rangeSlider)

        projectId = intent.getLongExtra("project_id", -1)

        CoroutineScope(Dispatchers.IO).launch {
            val project = AppDatabase.getDatabase(this@EditorActivity).projectDao().getProjectById(projectId)
            project?.let {
                withContext(Dispatchers.Main) {
                    videoUri = Uri.parse(it.videoUri)
                    videoView.setVideoURI(videoUri)
                    videoView.setOnPreparedListener { mp ->
                        videoDuration = mp.duration.toLong()
                        rangeSlider.valueTo = videoDuration.toFloat()
                        rangeSlider.values = listOf(0f, videoDuration.toFloat())
                    }
                }
            }
        }

        findViewById<View>(R.id.btnPlay).setOnClickListener {
            if (videoView.isPlaying) videoView.pause() else videoView.start()
        }

        findViewById<View>(R.id.btnExport).setOnClickListener {
            val intent = Intent(this, ExportSettingsActivity::class.java)
            intent.putExtra("project_id", projectId)
            intent.putExtra("video_uri", videoUri.toString())
            startActivity(intent)
        }
    }
}
