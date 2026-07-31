package com.montage.app.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.RangeSlider
import com.montage.app.R
import com.montage.app.data.db.AppDatabase
import com.montage.app.engine.ClipSegment
import com.montage.app.engine.VideoClipper
import com.montage.app.ui.export.ExportSettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var playerView: androidx.media3.ui.PlayerView
    private lateinit var rangeSlider: RangeSlider
    private lateinit var clipsContainer: LinearLayout
    private var player: ExoPlayer? = null
    private var videoUri: Uri? = null
    private var projectId: Long = -1
    private val clips = mutableListOf<ClipSegment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        playerView = findViewById(R.id.playerView)
        rangeSlider = findViewById(R.id.rangeSlider)
        clipsContainer = findViewById(R.id.clipsContainer)

        projectId = intent.getLongExtra("project_id", -1)
        setupPlayer()

        findViewById<MaterialButton>(R.id.btnPlay).setOnClickListener {
            player?.play()
        }

        findViewById<MaterialButton>(R.id.btnExport).setOnClickListener {
            if (clips.isNotEmpty()) {
                mergeAndExport()
            } else {
                videoUri?.let {
                    startExport(it)
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnSplit).setOnClickListener {
            splitClip()
        }

        findViewById<MaterialButton>(R.id.btnAddClip).setOnClickListener {
            addNewClip()
        }
    }

    private fun setupPlayer() {
        lifecycleScope.launch {
            val project = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@EditorActivity).projectDao().getProjectById(projectId)
            } ?: return@launch

            videoUri = Uri.parse(project.videoUri)
            clips.add(ClipSegment(videoUri!!, 0, Long.MAX_VALUE))
            updateTimelineUI()

            player = ExoPlayer.Builder(this@EditorActivity).build().also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.setMediaItem(MediaItem.fromUri(videoUri!!))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = false
            }
        }
    }

    private fun splitClip() {
        val currentPosition = player?.currentPosition ?: return
        val selectedClip = clips.find { currentPosition in it.startMs..it.endMs } ?: return

        val index = clips.indexOf(selectedClip)
        val firstPart = ClipSegment(selectedClip.uri, selectedClip.startMs, currentPosition)
        val secondPart = ClipSegment(selectedClip.uri, currentPosition, selectedClip.endMs)

        clips[index] = firstPart
        clips.add(index + 1, secondPart)
        updateTimelineUI()
        Toast.makeText(this, "تم التقسيم عند ${formatTime(currentPosition)}", Toast.LENGTH_SHORT).show()
    }

    private fun addNewClip() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clips.add(ClipSegment(uri, 0, Long.MAX_VALUE))
                updateTimelineUI()
                Toast.makeText(this, "تمت إضافة مقطع جديد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTimelineUI() {
        clipsContainer.removeAllViews()
        for ((index, clip) in clips.withIndex()) {
            val chip = TextView(this).apply {
                text = "مقطع ${index + 1}\n${formatTime(clip.startMs)} - ${formatTime(clip.endMs)}"
                textSize = 10f
                setTextColor(getColor(R.color.md_theme_on_surface))
                setPadding(16, 8, 16, 8)
                background = getDrawable(R.drawable.button_glass)
                setOnClickListener {
                    player?.seekTo(clip.startMs)
                }
            }
            clipsContainer.addView(chip)
        }
    }

    private fun mergeAndExport() {
        lifecycleScope.launch {
            Toast.makeText(this@EditorActivity, "جاري دمج المقاطع...", Toast.LENGTH_LONG).show()
            val outputFile = File(cacheDir, "merged_${System.currentTimeMillis()}.mp4")
            val success = VideoClipper.mergeClips(
                this@EditorActivity,
                clips,
                outputFile
            ) { progress ->
                runOnUiThread {
                    Toast.makeText(this@EditorActivity, "الدمج: $progress%", Toast.LENGTH_SHORT).show()
                }
            }
            if (success) {
                startExport(Uri.fromFile(outputFile))
            } else {
                Toast.makeText(this@EditorActivity, "فشل الدمج", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startExport(uri: Uri) {
        val intent = Intent(this, ExportSettingsActivity::class.java)
        intent.putExtra("project_id", projectId)
        intent.putExtra("video_uri", uri.toString())
        startActivity(intent)
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
