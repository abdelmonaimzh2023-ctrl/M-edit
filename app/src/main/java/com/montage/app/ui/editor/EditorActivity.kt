package com.montage.app.ui.editor

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.montage.app.R
import com.montage.app.data.db.AppDatabase
import kotlinx.coroutines.*

class EditorActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    private var videoDuration: Long = 0
    private var projectId: Long = -1
    private var videoUri: Uri? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        videoView = findViewById(R.id.videoView)
        seekBar = findViewById(R.id.seekBar)
        startTimeText = findViewById(R.id.startTime)
        endTimeText = findViewById(R.id.endTime)

        projectId = intent.getLongExtra("project_id", -1)

        // تحميل المشروع
        CoroutineScope(Dispatchers.IO).launch {
            val project = AppDatabase.getDatabase(this@EditorActivity).projectDao().getProjectById(projectId)
            project?.let {
                withContext(Dispatchers.Main) {
                    videoUri = Uri.parse(it.videoUri)
                    videoView.setVideoURI(videoUri)
                    loadVideoDuration()
                    setupControls()
                }
            }
        }
    }

    private fun loadVideoDuration() {
        videoUri?.let { uri ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                videoDuration = durationStr?.toLong() ?: 0
                seekBar.max = videoDuration.toInt()
                endTimeText.text = formatTime(videoDuration)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    private fun setupControls() {
        // زر التشغيل
        findViewById<LinearLayout>(R.id.btnPlay).setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
            } else {
                videoView.start()
                updateSeekBar()
            }
        }

        // شريط التقدم
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                }
                startTimeText.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })

        // زر التصدير
        findViewById<LinearLayout>(R.id.btnExport).setOnClickListener {
            Toast.makeText(this, "جاري التصدير...", Toast.LENGTH_SHORT).show()
            // هنا سنضيف محرك التصدير الحقيقي
        }

        // زر الرجوع
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun updateSeekBar() {
        if (videoView.isPlaying) {
            seekBar.progress = videoView.currentPosition
            startTimeText.text = formatTime(videoView.currentPosition.toLong())
            handler.postDelayed({ updateSeekBar() }, 500)
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}
